package app.nook.focus.repository.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FocusRangeStatsDto {

    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    @QueryProjection
    public FocusRangeStatsDto(LocalDateTime startedAt, LocalDateTime endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}
