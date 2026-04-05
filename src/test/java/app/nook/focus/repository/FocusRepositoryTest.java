package app.nook.focus.repository;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnableJpaRepositories(basePackages = "app.nook.focus.repository")
@EntityScan(basePackages = "app.nook")
@Import(QueryDslConfig.class)
public class FocusRepositoryTest {

    @Autowired
    private FocusRepository focusRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("유저의 진행 중인 포커스를 조회한다")
    void findByLibraryUserIdAndEndedAtIsNull_성공() {
        // given
        User user = User.builder()
                .email("test1@example.com")
                .nickName("테스터1")
                .provider("google")
                .providerId("provider-id-1")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder()
                .title("첫사랑의 침공")
                .author("권혁일")
                .build();
        em.persist(book);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME1)
                        .imageUrl("https://cdn.nook.com/themes/theme1.png")
                        .build()
        );

        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 3, 22, 14, 0, 0))
                .endedAt(null)
                .durationSec(0)
                .build();
        em.persist(focus);

        em.flush();
        em.clear();

        // when
        Optional<Focus> result = focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getLibrary().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getEndedAt()).isNull();
    }

    @Test
    @DisplayName("유저의 진행 중인 포커스가 없으면 빈 Optional을 반환한다")
    void findByLibraryUserIdAndEndedAtIsNull_없음() {
        // given
        User user = User.builder()
                .email("test2@example.com")
                .nickName("테스터2")
                .provider("google")
                .providerId("provider-id-2")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder()
                .title("행복할 거야 이래도 되나 싶을 정도로")
                .author("일홍")
                .build();
        em.persist(book);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME2)
                        .imageUrl("https://cdn.nook.com/themes/theme2.png")
                        .build()
        );

        Focus endedFocus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 3, 22, 14, 0, 0))
                .endedAt(LocalDateTime.of(2026, 3, 22, 14, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(endedFocus);

        em.flush();
        em.clear();

        // when
        Optional<Focus> result = focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("포커스 ID와 유저 ID로 포커스를 조회한다")
    void findByIdAndLibraryUserId_성공() {
        // given
        User user = User.builder()
                .email("test3@example.com")
                .nickName("테스터3")
                .provider("google")
                .providerId("provider-id-3")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder()
                .title("아몬드")
                .author("손원평")
                .build();
        em.persist(book);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME3)
                        .imageUrl("https://cdn.nook.com/themes/theme3.png")
                        .build()
        );

        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 3, 22, 15, 0, 0))
                .endedAt(null)
                .durationSec(0)
                .build();
        em.persist(focus);

        em.flush();
        em.clear();

        // when
        Optional<Focus> result = focusRepository.findByIdAndLibraryUserId(focus.getId(), user.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(focus.getId());
        assertThat(result.get().getLibrary().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("다른 유저의 포커스는 조회되지 않는다")
    void findByIdAndLibraryUserId_실패_다른유저() {
        // given
        User owner = User.builder()
                .email("owner@example.com")
                .nickName("소유자")
                .provider("google")
                .providerId("owner-provider")
                .role(UserRole.USER)
                .build();
        em.persist(owner);

        User otherUser = User.builder()
                .email("other@example.com")
                .nickName("다른유저")
                .provider("google")
                .providerId("other-provider")
                .role(UserRole.USER)
                .build();
        em.persist(otherUser);

        Book book = Book.builder()
                .title("불편한 편의점")
                .author("김호연")
                .build();
        em.persist(book);

        Library library = Library.builder()
                .user(owner)
                .book(book)
                .build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME1)
                        .imageUrl("https://cdn.nook.com/themes/theme1.png")
                        .build()
        );

        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 3, 22, 16, 0, 0))
                .endedAt(null)
                .durationSec(0)
                .build();
        em.persist(focus);

        em.flush();
        em.clear();

        // when
        Optional<Focus> result = focusRepository.findByIdAndLibraryUserId(focus.getId(), otherUser.getId());

        // then
        assertThat(result).isEmpty();
    }
}