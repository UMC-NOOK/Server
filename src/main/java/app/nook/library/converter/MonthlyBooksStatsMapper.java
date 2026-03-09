package app.nook.library.converter;

import app.nook.library.dto.DailyBookAggregateDto;
import app.nook.library.dto.FocusRankDto;
import app.nook.redis.dto.MonthlyBookCacheRow;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MonthlyBooksStatsMapper {

    private MonthlyBooksStatsMapper() {
    }

    public static List<DailyBookAggregateDto> toAggregates(Map<LocalDate, List<FocusRankDto.MonthlyFocusRow>> groupedByDate) {
        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<FocusRankDto.MonthlyFocusRow> dayRows = entry.getValue();

                    long bookCount = dayRows.size();
                    FocusRankDto.MonthlyFocusRow top = dayRows.stream()
                            .max(Comparator.comparingLong(FocusRankDto.MonthlyFocusRow::totalSec))
                            .orElse(null);

                    FocusRankDto.BookCalendarInfo topBook = null;
                    if (top != null) {
                        topBook = new FocusRankDto.BookCalendarInfo(top.bookId(), top.coverImageUrl());
                    }

                    long topFocusSec = top == null ? 0L : top.totalSec();
                    return new DailyBookAggregateDto(date, bookCount, topBook, topFocusSec);
                })
                .sorted(Comparator.comparingLong(DailyBookAggregateDto::topFocusSec).reversed()
                        .thenComparing(DailyBookAggregateDto::date))
                .toList();
    }

    public static List<FocusRankDto.DailyBookItem> toDailyBookItems(List<DailyBookAggregateDto> aggregates) {
        return aggregates.stream()
                .map(item -> new FocusRankDto.DailyBookItem(item.date(), item.bookCount(), item.topBook()))
                .toList();
    }

    public static List<MonthlyBookCacheRow> toCacheRows(List<DailyBookAggregateDto> aggregates) {
        return aggregates.stream()
                .map(item -> new MonthlyBookCacheRow(
                        item.date(),
                        item.bookCount(),
                        item.topBook() == null ? null : item.topBook().bookId(),
                        item.topBook() == null ? null : item.topBook().coverUrl(),
                        item.topFocusSec()
                ))
                .toList();
    }
}
