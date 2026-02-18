package app.nook.focus.dto;

import app.nook.focus.domain.enums.ThemeName;

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
}
