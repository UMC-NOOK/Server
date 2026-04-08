package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.library.converter.MonthlyBooksStatsMapper;
import app.nook.library.dto.DailyBookAggregateDto;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.MonthlyBooksQueryResultDto;
import app.nook.r2.service.PresignedUrlService;
import app.nook.redis.dto.MonthlyBookCacheRow;
import app.nook.redis.exception.RedisOperationException;
import app.nook.redis.service.RedisZSETService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
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

    // 서재 월별 책 조회 Redis ZSET 캐시 확인, 미스 시 DB 조회
    public FocusRankDto.MonthlyBooksResponseDto viewMonthly(Long userId, YearMonth yearMonth) {
        // Redis ZSET 캐시 확인
        FocusRankDto.MonthlyBooksResponseDto cached = safeLoadMonthlyBooks(userId, yearMonth);
        if (cached != null) {
            return cached;
        }

        // 캐시 미스 시 DB 조회
        MonthlyBooksQueryResultDto queryResult = loadMonthlyBooksFromDB(userId, yearMonth);

        // Redis ZSET에 저장(score=topFocusSec)
        safeSaveMonthlyBooks(
                userId,
                yearMonth,
                queryResult.totalBookCount(),
                queryResult.cacheRows()
        );

        return queryResult.response();
    }

    // 캐시 미스 시 DB에서 조회하는 메서드
    private MonthlyBooksQueryResultDto loadMonthlyBooksFromDB(Long userId, YearMonth yearMonth) {
        // 월 범위 계산
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

        // 집계 결과 받아오기
        List<FocusRankDto.MonthlyFocusRow> rows = focusRepository.findMonthlyFocusStats(userId, startDate, endDate)
                .stream()
                .map(row -> new FocusRankDto.MonthlyFocusRow(
                        row.getFocusDate(),
                        row.getBookId(),
                        presignedUrlService.resolveImageUrl(userId, row.getCoverImageUrl()),
                        row.getTotalSec()
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
        List<FocusRankDto.FocusTimeRow> rows = safeLoadMonthlyFocusTime(userId, yearMonth);
        // 캐시 미스 시 DB 조회
        if (rows == null) {
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

            rows = focusRepository.findFocusTimeStats(userId, startDate, endDate)
                    .stream()
                    .map(row -> new FocusRankDto.FocusTimeRow(
                            row.getFocusDate(),
                            row.getTotalSec()
                    ))
                    .toList();
            safeSaveMonthlyFocusTime(userId, yearMonth, rows);
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

}
