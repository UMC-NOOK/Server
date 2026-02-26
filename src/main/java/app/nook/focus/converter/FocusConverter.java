package app.nook.focus.converter;

import app.nook.focus.domain.Theme;
import app.nook.focus.dto.FocusResponseDto;

import java.util.List;

public class FocusConverter {

    public static FocusResponseDto.ThemeListDto toThemeListDto(List<Theme> themes) {
        List<FocusResponseDto.ThemeItemDto> items = themes.stream()
                .map(t -> new FocusResponseDto.ThemeItemDto(
                        t.getId(),
                        t.getName().name(),
                        t.getImageUrl()
                ))
                .toList();

        return new FocusResponseDto.ThemeListDto(items);
    }
}
