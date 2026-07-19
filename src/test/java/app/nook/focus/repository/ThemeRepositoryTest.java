package app.nook.focus.repository;

import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaRepositories(basePackages = "app.nook.focus.repository")
@EntityScan(basePackages = "app.nook")
@Import(QueryDslConfig.class)
public class ThemeRepositoryTest extends AbstractPostgresContainerTests {

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("테마 목록을 ID 오름차순으로 조회한다")
    void findAllByOrderByIdAsc_성공() {
        // given
        Theme t1 = themeRepository.save(Theme.builder()
                .name(ThemeName.THEME2)
                .imageUrl("https://cdn.nook.com/themes/theme2.png")
                .build());

        Theme t2 = themeRepository.save(Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build());

        // when
        List<Theme> result = themeRepository.findAllByOrderByIdAsc();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(t1.getId());
        assertThat(result.get(1).getId()).isEqualTo(t2.getId());
    }
}
