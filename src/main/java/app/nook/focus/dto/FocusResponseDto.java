package app.nook.focus.dto;

import java.time.LocalDateTime;

public class FocusResponseDto {

    public record FocusStart(
            Long focusId,
            Long bookId,
            String bookTitle,
            String author,
            LocalDateTime startedAt
    ) {}

    public record FocusEnd(
            Long focusId,
            Long bookId,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer durationSec,
            String durationText,
            Integer page,
            Long totalFocusSec,
            String readingStatus
    ) {}

    public record RecentFocusItem(
            Long focusId,
            Long bookId,
            String bookTitle,
            String author,
            String coverImageUrl,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String durationText
    ) {}
}
