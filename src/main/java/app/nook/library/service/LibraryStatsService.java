package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.focus.service.FocusDailyTimeCalculator;
import app.nook.library.converter.MonthlyBooksStatsMapper;
import app.nook.library.dto.DailyBookAggregateDto;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.MonthlyBooksQueryResultDto;
import app.nook.library.util.LibraryFocusUtil;
import app.nook.r2.service.PresignedUrlService;
import app.nook.redis.dto.MonthlyBookCacheRow;
import app.nook.redis.exception.RedisOperationException;
import app.nook.redis.service.RedisZSETService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryStatsService {

    private final FocusRepository focusRepository;
    private final RedisZSETService redisZSETService;
    private final PresignedUrlService presignedUrlService;
    private final FocusDailyTimeCalculator focusDailyTimeCalculator;
    private final Clock clock;

    // 서재 월별 책 조회 Redis ZSET 캐시 확인, 미스 시 DB 조회
    public FocusRankDto.MonthlyBooksResponseDto viewMonthly(Long userId, YearMonth yearMonth) {
        LocalDateTime serverNow = LocalDateTime.now(clock);
        boolean useCache = canUseMonthlyCache(userId, yearMonth, serverNow);

        // Redis ZSET 캐시 확인
        FocusRankDto.MonthlyBooksResponseDto cached = useCache
                ? safeLoadMonthlyBooks(userId, yearMonth)
                : null;
        if (cached != null) {
            return resolveMonthlyBookImageUrls(userId, cached);
        }

        // 캐시 미스 시 DB 조회
        MonthlyBooksQueryResultDto queryResult = loadMonthlyBooksFromDB(userId, yearMonth, serverNow);

        // Redis ZSET에 저장(score=topFocusSec)
        if (useCache) {
            safeSaveMonthlyBooks(
                    userId,
                    yearMonth,
                    queryResult.totalBookCount(),
                    queryResult.cacheRows()
            );
        }

        return resolveMonthlyBookImageUrls(userId, queryResult.response());
    }

    // 캐시 미스 시 DB에서 조회하는 메서드
    private MonthlyBooksQueryResultDto loadMonthlyBooksFromDB(
            Long userId,
            YearMonth yearMonth,
            LocalDateTime serverNow
    ) {
        // 월 범위 계산
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atStartOfDay();

        Map<DailyBookKey, Long> totalSecByDateAndBook = new HashMap<>();
        for (FocusRangeStatsDto focus : focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd)) {
            for (FocusDailyTimeCalculator.DailyFocusTime segment : focusDailyTimeCalculator.splitByDate(
                    focus.getStartedAt(),
                    focus.getEndedAt(),
                    serverNow,
                    rangeStart,
                    rangeEnd
            )) {
                DailyBookKey key = new DailyBookKey(
                        segment.date(),
                        focus.getBookId(),
                        focus.getCoverImageKey()
                );
                totalSecByDateAndBook.merge(key, segment.durationSec(), Long::sum);
            }
        }

        List<FocusRankDto.MonthlyFocusRow> rows = totalSecByDateAndBook.entrySet().stream()
                .map(entry -> new FocusRankDto.MonthlyFocusRow(
                        entry.getKey().date(),
                        entry.getKey().bookId(),
                        entry.getKey().coverImageKey(),
                        entry.getValue()
                ))
                .toList();

        int totalBookCount = rows.size();

        // 날짜별로 다시 그룹화
        Map<LocalDate, List<FocusRankDto.MonthlyFocusRow>> groupedByDate = rows.stream()
                .collect(
                        Collectors.groupingBy(FocusRankDto.MonthlyFocusRow::date)
                );

        // 날짜별 top focusSec를 같이 계산해 정렬 기준으로 사용
        List<DailyBookAggregateDto> aggregates = MonthlyBooksStatsMapper.toAggregates(groupedByDate);

        List<FocusRankDto.DailyBookItem> dailyBookItems = MonthlyBooksStatsMapper.toDailyBookItems(aggregates);

        List<MonthlyBookCacheRow> cacheRows = MonthlyBooksStatsMapper.toCacheRows(aggregates);

        FocusRankDto.MonthlyBooksResponseDto response =
                new FocusRankDto.MonthlyBooksResponseDto(yearMonth, totalBookCount, dailyBookItems);
        return new MonthlyBooksQueryResultDto(totalBookCount, response, cacheRows);
    }

    // 서재 월별 포커스 시간 통계 조회 (Redis ZSET 캐시 → 미스 시 DB 조회)
    public FocusRankDto.FocusBookResponseDto viewFocusTimeStats(Long userId, YearMonth yearMonth) {
        LocalDateTime serverNow = LocalDateTime.now(clock);
        boolean useCache = canUseMonthlyCache(userId, yearMonth, serverNow);
        List<FocusRankDto.FocusTimeRow> rows = useCache
                ? safeLoadMonthlyFocusTime(userId, yearMonth)
                : null;
        // 캐시 미스 시 DB 조회
        if (rows == null) {
            rows = loadMonthlyFocusTimeFromDB(userId, yearMonth, serverNow);
            if (useCache) {
                safeSaveMonthlyFocusTime(userId, yearMonth, rows);
            }
        }

        long totalSec = rows.stream().mapToLong(FocusRankDto.FocusTimeRow::totalSec).sum();
        int totalFocusMin = (int) (totalSec / 60);
        long maxDaySec = rows.stream()
                .mapToLong(FocusRankDto.FocusTimeRow::totalSec)
                .max()
                .orElse(0L);


        List<FocusRankDto.FocusDateItem> focusBookItems = rows.stream()
                .sorted(Comparator.comparing(FocusRankDto.FocusTimeRow::date))
                .map(row -> new FocusRankDto.FocusDateItem(
                        row.date(),
                        LibraryFocusUtil.toFocusTimeSlot(row.totalSec(), maxDaySec)
                ))
                .toList();

        return new FocusRankDto.FocusBookResponseDto(yearMonth, totalFocusMin, focusBookItems);
    }

    private List<FocusRankDto.FocusTimeRow> loadMonthlyFocusTimeFromDB(
            Long userId,
            YearMonth yearMonth,
            LocalDateTime serverNow
    ) {
        LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<LocalDate, Long> totalSecByDate = new HashMap<>();

        for (FocusRangeStatsDto focus : focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd)) {
            for (FocusDailyTimeCalculator.DailyFocusTime segment : focusDailyTimeCalculator.splitByDate(
                    focus.getStartedAt(),
                    focus.getEndedAt(),
                    serverNow,
                    rangeStart,
                    rangeEnd
            )) {
                totalSecByDate.merge(segment.date(), segment.durationSec(), Long::sum);
            }
        }

        return totalSecByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new FocusRankDto.FocusTimeRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean canUseMonthlyCache(Long userId, YearMonth yearMonth, LocalDateTime serverNow) {
        LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        return focusRepository.findByLibraryUserIdAndEndedAtIsNull(userId)
                .map(focus -> !focusDailyTimeCalculator.overlaps(
                        focus.getStartedAt(),
                        focus.getEndedAt(),
                        serverNow,
                        rangeStart,
                        rangeEnd
                ))
                .orElse(true);
    }

    private FocusRankDto.MonthlyBooksResponseDto safeLoadMonthlyBooks(Long userId, YearMonth yearMonth) {
        try {
            return redisZSETService.loadMonthlyBooks(userId, yearMonth);
        } catch (RedisOperationException e) {
            log.warn("Redis loadMonthlyBooks failed. userId={}, yearMonth={}", userId, yearMonth, e);
            return null;
        }
    }

    private void safeSaveMonthlyBooks(
            Long userId,
            YearMonth yearMonth,
            int totalBookCount,
            List<MonthlyBookCacheRow> rows
    ) {
        try {
            redisZSETService.saveMonthlyBooks(userId, yearMonth, totalBookCount, rows);
        } catch (RedisOperationException e) {
            log.warn("Redis saveMonthlyBooks failed. userId={}, yearMonth={}", userId, yearMonth, e);
        }
    }

    private List<FocusRankDto.FocusTimeRow> safeLoadMonthlyFocusTime(Long userId, YearMonth yearMonth) {
        try {
            return redisZSETService.loadMonthlyFocusTime(userId, yearMonth);
        } catch (RedisOperationException e) {
            log.warn("Redis loadMonthlyFocusTime failed. userId={}, yearMonth={}", userId, yearMonth, e);
            return null;
        }
    }

    private void safeSaveMonthlyFocusTime(Long userId, YearMonth yearMonth, List<FocusRankDto.FocusTimeRow> rows) {
        try {
            redisZSETService.saveMonthlyFocusTime(userId, yearMonth, rows);
        } catch (RedisOperationException e) {
            log.warn("Redis saveMonthlyFocusTime failed. userId={}, yearMonth={}", userId, yearMonth, e);
        }
    }

    private FocusRankDto.MonthlyBooksResponseDto resolveMonthlyBookImageUrls(
            Long userId,
            FocusRankDto.MonthlyBooksResponseDto response
    ) {
        List<FocusRankDto.DailyBookItem> resolvedDays = response.days().stream()
                .map(day -> {
                    FocusRankDto.BookCalendarInfo topBook = day.topBook();
                    if (topBook == null) {
                        return day;
                    }

                    String resolvedCoverUrl = presignedUrlService.resolveImageUrl(userId, topBook.coverUrl());

                    return new FocusRankDto.DailyBookItem(
                            day.date(),
                            day.bookCount(),
                            new FocusRankDto.BookCalendarInfo(
                                    topBook.bookId(),
                                    resolvedCoverUrl
                            )
                    );
                })
                .toList();

        return new FocusRankDto.MonthlyBooksResponseDto(
                response.yearMonth(),
                response.totalBookCount(),
                resolvedDays
        );
    }

    private record DailyBookKey(LocalDate date, Long bookId, String coverImageKey) {
    }

}
