package app.nook.focus.service;

import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.ThemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    @Mock
    private ThemeRepository themeRepository;

    @InjectMocks
    private ThemeService themeService;

    private Theme theme1;
    private Theme theme2;

    @BeforeEach
    void setUp() {
        theme1 = Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build();
        ReflectionTestUtils.setField(theme1, "id", 1L);

        theme2 = Theme.builder()
                .name(ThemeName.THEME2)
                .imageUrl("https://cdn.nook.com/themes/theme2.png")
                .build();
        ReflectionTestUtils.setField(theme2, "id", 2L);
    }

    @Test
    @DisplayName("테마 목록 조회 성공 - DTO 매핑 검증")
    void getThemes_성공() {
        // given
        given(themeRepository.findAllByOrderByIdAsc()).willReturn(List.of(theme1, theme2));

        // when
        FocusResponseDto.ThemeListDto result = themeService.getThemes();

        // then
        assertThat(result).isNotNull();
        assertThat(result.themes()).hasSize(2);

        assertThat(result.themes().get(0).themeId()).isEqualTo(1L);
        assertThat(result.themes().get(0).name()).isEqualTo("THEME1");
        assertThat(result.themes().get(0).imageUrl()).isEqualTo("https://cdn.nook.com/themes/theme1.png");

        assertThat(result.themes().get(1).themeId()).isEqualTo(2L);
        assertThat(result.themes().get(1).name()).isEqualTo("THEME2");
        assertThat(result.themes().get(1).imageUrl()).isEqualTo("https://cdn.nook.com/themes/theme2.png");
    }

    @Test
    @DisplayName("테마가 비어있으면 빈 리스트 반환")
    void getThemes_빈리스트() {
        // given
        given(themeRepository.findAllByOrderByIdAsc()).willReturn(List.of());

        // when
        FocusResponseDto.ThemeListDto result = themeService.getThemes();

        // then
        assertThat(result.themes()).isEmpty();
    }
}
