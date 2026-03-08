package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.library.dto.FocusRankDto;
import app.nook.redis.service.RedisZSETService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryStatsService {

    private final FocusRepository focusRepository;
    private final RedisZSETService redisZSETService;

    // 서재 월별 책 조회 Redis ZSET 캐시 확인, 미스 시 DB 조회
    public FocusRankDto.MonthlyBooksResponseDto viewMonthly(Long userId, YearMonth yearMonth) {
        // Redis ZSET 캐시 확인
        FocusRankDto.MonthlyBooksResponseDto cached = redisZSETService.loadMonthlyBooks(userId, yearMonth);
        if (cached != null) {
            return cached;
        }

        // 캐시 미스 시 DB 조회
        FocusRankDto.MonthlyBooksResponseDto response = loadMonthlyBooksFromDB(userId, yearMonth);

        // Redis ZSET에 저장
        redisZSETService.saveMonthlyBooks(userId, yearMonth, response);

        return response;
    }

    private FocusRankDto.MonthlyBooksResponseDto loadMonthlyBooksFromDB(Long userId, YearMonth yearMonth) {
        // 월 범위 계산
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

        // 집계 결과 받아오기
        List<FocusRankDto.MonthlyFocusRow> rows = focusRepository.findMonthlyFocusStats(userId, startDate, endDate)
                .stream()
                .map(row -> new FocusRankDto.MonthlyFocusRow(
                        row.getFocusDate(),
                        row.getBookId(),
                        row.getCoverImageUrl(),
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
        record DailyBookAggregate(
                LocalDate date,
                long bookCount,
                FocusRankDto.BookCalendarInfo topBook,
                long topFocusSec
        ) {
        }

        List<FocusRankDto.DailyBookItem> dailyBookItems = groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<FocusRankDto.MonthlyFocusRow> dayRows = entry.getValue();

                    // 날짜별 다른 책 개수
                    long bookCount = dayRows.size();

                    // 가장 오래 읽은 책
                    FocusRankDto.MonthlyFocusRow top = dayRows.stream()
                            .max(Comparator.comparingLong(FocusRankDto.MonthlyFocusRow::totalSec))
                            .orElse(null);

                    FocusRankDto.BookCalendarInfo topBook = null;

                    // 오래 읽은 책이 있으면 매핑
                    if (top!=null) {
                        topBook = new FocusRankDto.BookCalendarInfo(
                                top.bookId(),
                                top.coverImageUrl()
                        );
                    }
                    long topFocusSec = top == null ? 0L : top.totalSec();
                    return new DailyBookAggregate(date, bookCount, topBook, topFocusSec);
                })
               // 오래 읽은 책 내림차순 정렬
                .sorted(Comparator.comparingLong(DailyBookAggregate::topFocusSec).reversed()
                        .thenComparing(DailyBookAggregate::date))
                .map(item -> new FocusRankDto.DailyBookItem(
                        item.date(),
                        item.bookCount(),
                        item.topBook()
                ))
                .toList();
        return new FocusRankDto.MonthlyBooksResponseDto(yearMonth, totalBookCount, dailyBookItems);
    }

    // 서재 월별 포커스 시간 통계 조회 (Redis ZSET 캐시 → 미스 시 DB 조회)
    public FocusRankDto.FocusBookResponseDto viewFocusTimeStats(Long userId, YearMonth yearMonth) {
        List<FocusRankDto.FocusTimeRow> rows = redisZSETService.loadMonthlyFocusTime(userId, yearMonth);
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
            redisZSETService.saveMonthlyFocusTime(userId, yearMonth, rows);
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
}
