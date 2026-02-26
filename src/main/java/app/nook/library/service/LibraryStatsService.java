package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.library.dto.FocusRankDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 서재 월별 책 조회
    @Cacheable(
            value = "libraryMonthlyCurrent",
            key = "#userId + ':' + #yearMonth"
    )
    public FocusRankDto.MonthlyBooksResponseDto viewMonthly(Long userId, YearMonth yearMonth) {
        // 월 범위 계산
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        // 집계 결과 받아오기
        List<FocusRankDto.MonthlyFocusRow> rows = focusRepository.findMonthlyFocusStats(userId, start, end)
                .stream()
                .map(row -> new FocusRankDto.MonthlyFocusRow(
                        LocalDate.of(row.getYearValue(), row.getMonthValue(), row.getDayValue()),
                        row.getBookId(),
                        row.getCoverImageUrl(),
                        row.getTotalSec()
                ))
                .toList();

        // 월 전체 초를 분으로 변환
        long totalSec = rows.stream()
                .mapToLong(FocusRankDto.MonthlyFocusRow::totalSec)
                .sum();
        int totalFocusMin = (int) (totalSec / 60);

        // 날짜별로 다시 그룹화
        Map<LocalDate, List<FocusRankDto.MonthlyFocusRow>> groupedByDate = rows.stream()
                .collect(
                        Collectors.groupingBy(FocusRankDto.MonthlyFocusRow::date)
                );

        // DailyBookItem으로 매핑
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
                    return new FocusRankDto.DailyBookItem(date, bookCount, topBook);
                })
                .sorted(Comparator.comparing(FocusRankDto.DailyBookItem::date)) // 오름차순 정렬
                .toList();
        return new FocusRankDto.MonthlyBooksResponseDto(yearMonth,totalFocusMin,dailyBookItems);
    }

    // 서재 월별 포커스 시간 통계 조회
    @Cacheable(
            value = "focusMonthlyCurrent",
            key = "#userId + ':' + #yearMonth"
    )
    public FocusRankDto.FocusBookResponseDto viewFocusTimeStats(Long userId, YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<FocusRankDto.FocusTimeRow> rows = focusRepository.findFocusTimeStats(userId, start, end)
                .stream()
                .map(row -> new FocusRankDto.FocusTimeRow(
                        LocalDate.of(row.getYearValue(), row.getMonthValue(), row.getDayValue()),
                        row.getTotalSec()
                ))
                .toList();

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
