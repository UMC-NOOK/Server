package app.nook.focus.repository.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MonthlyFocusStatsDto {

    private final LocalDate focusDate;
    private final Long bookId;
    private final String coverImageUrl;
    private final Long totalSec;

    @QueryProjection
    public MonthlyFocusStatsDto(LocalDate focusDate, Long bookId, String coverImageUrl, Long totalSec) {
        this.focusDate = focusDate;
        this.bookId = bookId;
        this.coverImageUrl = coverImageUrl;
        this.totalSec = totalSec;
    }
}
