package app.nook.focus.service;

import app.nook.focus.converter.FocusConverter;
import app.nook.focus.domain.Theme;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;

    public FocusResponseDto.ThemeListDto getThemes() {
        List<Theme> themes = themeRepository.findAllByOrderByIdAsc();
        return FocusConverter.toThemeListDto(themes);
    }

}
