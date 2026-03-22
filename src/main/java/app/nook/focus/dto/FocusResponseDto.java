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
}
