package app.nook.global.fixture;

import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import org.springframework.test.util.ReflectionTestUtils;

public class ThemeFixture {

    public static Theme theme() {
        Theme theme = Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build();
        ReflectionTestUtils.setField(theme, "id", 1L);
        return theme;
    }
}
