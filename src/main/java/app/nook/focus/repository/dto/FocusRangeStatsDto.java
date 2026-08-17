package app.nook.focus.repository.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FocusRangeStatsDto {

    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final Long bookId;
    private final String coverImageKey;

    @QueryProjection
    public FocusRangeStatsDto(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Long bookId,
            String coverImageKey
    ) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.bookId = bookId;
        this.coverImageKey = coverImageKey;
    }
}
