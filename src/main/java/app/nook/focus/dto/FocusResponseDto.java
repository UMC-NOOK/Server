package app.nook.focus.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FocusResponseDto {

    public record ThemeItemDto(
            Long themeId,
            String name,
            String imageUrl
    ) {}

    public record ThemeListDto(
            List<ThemeItemDto> themes
    ) {}

    public record FocusStart(
            Long focusId,
            Long libraryId,
            Long bookId,
            String bookTitle,
            String author,
            Long themeId,
            String themeName,
            LocalDateTime startedAt

    ) {}

    public record FocusEnd(
            Long focusId,
            Long libraryId,
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
