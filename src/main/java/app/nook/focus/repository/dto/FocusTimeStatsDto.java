package app.nook.focus.repository.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class FocusTimeStatsDto {

    private final LocalDate focusDate;
    private final Long totalSec;

    @QueryProjection
    public FocusTimeStatsDto(LocalDate focusDate, Long totalSec) {
        this.focusDate = focusDate;
        this.totalSec = totalSec;
    }
}
