package app.nook.library.dto;

import java.time.LocalDate;

public record DailyBookAggregateDto(
        LocalDate date,
        long bookCount,
        FocusRankDto.BookCalendarInfo topBook,
        long topFocusSec
) {
}
