package app.nook.focus.repository;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaRepositories(basePackages = "app.nook.focus.repository")
@EntityScan(basePackages = "app.nook")
@Import(QueryDslConfig.class)
public class FocusRepositoryTest extends AbstractPostgresContainerTests {

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
    void findAllByLibraryIdAndLibraryUserId_filtersByOwnership() {
        User owner = persistUser("focus-owner");
        User otherUser = persistUser("focus-other-user");
        Book ownerBook = persistBook("소유자 책", null);
        Book otherBook = persistBook("다른 사용자 책", null);
        Library ownerLibrary = persistLibrary(owner, ownerBook);
        Library otherLibrary = persistLibrary(otherUser, otherBook);
        Theme theme = persistTheme();
        Focus ownedFocus = persistFocus(
                ownerLibrary,
                theme,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 10, 30)
        );
        persistFocus(
                otherLibrary,
                theme,
                LocalDateTime.of(2026, 3, 1, 11, 0),
                LocalDateTime.of(2026, 3, 1, 11, 30)
        );
        em.flush();
        em.clear();

        List<Focus> ownedFocuses = focusRepository.findAllByLibraryIdAndLibraryUserId(ownerLibrary.getId(), owner.getId());
        List<Focus> otherUsersFocuses = focusRepository.findAllByLibraryIdAndLibraryUserId(
                ownerLibrary.getId(),
                otherUser.getId()
        );

        assertThat(ownedFocuses).extracting(Focus::getId).containsExactly(ownedFocus.getId());
        assertThat(otherUsersFocuses).isEmpty();
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
    @Transactional
    void findByIdAndLibraryUserIdForUpdate_returnsOwnedFocusInsideTransaction() {
        User owner = persistUser("end-lock-owner");
        Book book = persistBook("종료 잠금 책", null);
        Library library = persistLibrary(owner, book);
        Theme theme = persistTheme();
        Focus focus = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                null
        );
        em.flush();
        em.clear();

        Optional<Focus> result = focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), owner.getId());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(focus.getId());
        assertThat(result.orElseThrow().getLibrary().getUser().getId()).isEqualTo(owner.getId());
    }

    @Test
    @Transactional
    void findByIdAndLibraryUserIdForUpdate_rejectsAnotherUser() {
        User owner = persistUser("end-lock-owner-only");
        User otherUser = persistUser("end-lock-other-user");
        Book book = persistBook("다른 사용자 종료 잠금 책", null);
        Library library = persistLibrary(owner, book);
        Theme theme = persistTheme();
        Focus focus = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                null
        );
        em.flush();
        em.clear();

        Optional<Focus> result = focusRepository.findByIdAndLibraryUserIdForUpdate(
                focus.getId(),
                otherUser.getId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndLibraryUserIdForUpdate_declaresPessimisticWriteQuery() throws NoSuchMethodException {
        java.lang.reflect.Method method = FocusRepository.class.getMethod(
                "findByIdAndLibraryUserIdForUpdate",
                Long.class,
                Long.class
        );

        Lock lock = method.getAnnotation(Lock.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(query).isNotNull();
        assertThat(query.value()).contains("user.id");
    }

    @Test
    void countByLibraryAndEndedAtIsNotNull_countsOnlyCompletedFocuses() {
        User user = persistUser("completed-focus-count");
        Book book = persistBook("완료 포커스 카운트 책", null);
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 9, 30)
        );
        persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 10, 30)
        );
        persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 20, 11, 0),
                null
        );
        em.flush();
        em.clear();

        int completedFocusCount = focusRepository.countByLibraryAndEndedAtIsNotNull(library);

        assertThat(completedFocusCount).isEqualTo(2);
    }

    @Test
    void focusDurationTooShortError_isAConflict() {
        assertThat(FocusErrorCode.FOCUS_DURATION_TOO_SHORT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("완료된 포커스를 id 내림차순으로 커서 없이 조회한다")
    void findRecentByUserWithCursor_첫페이지() {
        User user = User.builder()
                .email("recent-cursor@example.com")
                .nickName("커서유저")
                .provider("google")
                .providerId("cursor-provider")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder().title("테스트 책").author("작가").build();
        em.persist(book);

        Library library = Library.builder().user(user).book(book).build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME1)
                        .imageUrl("https://cdn.nook.com/themes/theme1.png")
                        .build()
        );

        Focus older = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 1, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(older);

        Focus newer = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 2, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 2, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(newer);

        // endedAt == null 인 진행 중 포커스 (결과에 포함되면 안 됨)
        Focus inProgress = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 3, 10, 0, 0))
                .endedAt(null)
                .durationSec(0)
                .build();
        em.persist(inProgress);

        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findRecentByUserWithCursor(user, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(older.getId());
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("커서 이전 id의 포커스만 조회된다")
    void findRecentByUserWithCursor_커서페이지() {
        User user = User.builder()
                .email("recent-cursor2@example.com")
                .nickName("커서유저2")
                .provider("google")
                .providerId("cursor-provider-2")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder().title("테스트 책2").author("작가2").build();
        em.persist(book);

        Library library = Library.builder().user(user).book(book).build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME2)
                        .imageUrl("https://cdn.nook.com/themes/theme2.png")
                        .build()
        );

        Focus focus1 = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 5, 1, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(focus1);

        Focus focus2 = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 5, 2, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 5, 2, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(focus2);

        Focus focus3 = Focus.builder()
                .library(library).theme(theme)
                .startedAt(LocalDateTime.of(2026, 5, 3, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 5, 3, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(focus3);

        em.flush();
        em.clear();

        // focus3.id를 커서로 넘기면 focus3보다 id가 작은 것들만 나와야 함
        Slice<Focus> result = focusRepository.findRecentByUserWithCursor(user, focus3.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).noneMatch(f -> f.getId().equals(focus3.getId()));
        assertThat(result.getContent().get(0).getId()).isEqualTo(focus2.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(focus1.getId());
    }

    @Test
    @DisplayName("일별 완료 행은 크기 1 커서에서 id 내림차순으로 한 번씩만 조회된다")
    void findRecentByUserWithCursor_dailyRowsSizeOneHaveNoOmissionOrDuplication() {
        User user = persistUser("daily-recent-cursor-owner");
        Book book = persistBook("일별 최근 포커스 책", "book/users/1/daily-recent.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        Focus beforeMidnight = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 55),
                LocalDateTime.of(2026, 8, 2, 0, 0)
        );
        Focus afterMidnight = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 2, 0, 0),
                LocalDateTime.of(2026, 8, 2, 0, 10)
        );
        em.flush();
        em.clear();

        Slice<Focus> firstPage = focusRepository.findRecentByUserWithCursor(user, null, PageRequest.of(0, 1));
        Slice<Focus> secondPage = focusRepository.findRecentByUserWithCursor(
                user,
                firstPage.getContent().get(0).getId(),
                PageRequest.of(0, 1)
        );

        assertThat(firstPage.getContent()).extracting(Focus::getId).containsExactly(afterMidnight.getId());
        assertThat(firstPage.getContent()).extracting(Focus::getDurationSec).containsExactly(600);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).extracting(Focus::getId).containsExactly(beforeMidnight.getId());
        assertThat(secondPage.getContent()).extracting(Focus::getDurationSec).containsExactly(300);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("페이지 크기보다 많으면 hasNext가 true다")
    void findRecentByUserWithCursor_hasNext() {
        User user = User.builder()
                .email("recent-cursor3@example.com")
                .nickName("커서유저3")
                .provider("google")
                .providerId("cursor-provider-3")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book book = Book.builder().title("테스트 책3").author("작가3").build();
        em.persist(book);

        Library library = Library.builder().user(user).book(book).build();
        em.persist(library);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME3)
                        .imageUrl("https://cdn.nook.com/themes/theme3.png")
                        .build()
        );

        for (int i = 1; i <= 3; i++) {
            em.persist(Focus.builder()
                    .library(library).theme(theme)
                    .startedAt(LocalDateTime.of(2026, 6, i, 10, 0, 0))
                    .endedAt(LocalDateTime.of(2026, 6, i, 10, 30, 0))
                    .durationSec(1800)
                    .build());
        }

        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findRecentByUserWithCursor(user, null, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다른 유저의 포커스는 조회되지 않는다")
    void findRecentByUserWithCursor_다른유저_제외() {
        User owner = User.builder()
                .email("recent-owner@example.com")
                .nickName("소유자")
                .provider("google")
                .providerId("owner-provider-2")
                .role(UserRole.USER)
                .build();
        em.persist(owner);

        User other = User.builder()
                .email("recent-other@example.com")
                .nickName("다른유저")
                .provider("google")
                .providerId("other-provider-2")
                .role(UserRole.USER)
                .build();
        em.persist(other);

        Book book = Book.builder().title("공유 책").author("작가").build();
        em.persist(book);

        Library ownerLibrary = Library.builder().user(owner).book(book).build();
        em.persist(ownerLibrary);

        Library otherLibrary = Library.builder().user(other).book(book).build();
        em.persist(otherLibrary);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME1)
                        .imageUrl("https://cdn.nook.com/themes/theme1.png")
                        .build()
        );

        em.persist(Focus.builder()
                .library(ownerLibrary).theme(theme)
                .startedAt(LocalDateTime.of(2026, 7, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 7, 1, 10, 30, 0))
                .durationSec(1800)
                .build());

        em.persist(Focus.builder()
                .library(otherLibrary).theme(theme)
                .startedAt(LocalDateTime.of(2026, 7, 1, 11, 0, 0))
                .endedAt(LocalDateTime.of(2026, 7, 1, 11, 30, 0))
                .durationSec(1800)
                .build());

        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findRecentByUserWithCursor(owner, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLibrary().getUser().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("유저별 최근 포커스 책을 startedAt 기준으로 중복 없이 최신순 조회한다")
    void findRecentDistinctBooksByUser_성공() {
        User user = User.builder()
                .email("focus-books@example.com")
                .nickName("포커스유저")
                .provider("google")
                .providerId("focus-provider")
                .role(UserRole.USER)
                .build();
        em.persist(user);

        Book bookA = Book.builder()
                .title("A 책")
                .author("작가A")
                .coverImageKey("book/users/1/a.jpg")
                .build();
        em.persist(bookA);

        Book bookB = Book.builder()
                .title("B 책")
                .author("작가B")
                .coverImageKey("book/users/1/b.jpg")
                .build();
        em.persist(bookB);

        Library libraryA = Library.builder()
                .user(user)
                .book(bookA)
                .build();
        em.persist(libraryA);

        Library libraryB = Library.builder()
                .user(user)
                .book(bookB)
                .build();
        em.persist(libraryB);

        Theme theme = themeRepository.save(
                Theme.builder()
                        .name(ThemeName.THEME1)
                        .imageUrl("https://cdn.nook.com/themes/theme1.png")
                        .build()
        );

        Focus firstA = Focus.builder()
                .library(libraryA)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 11, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(firstA);

        Focus focusB = Focus.builder()
                .library(libraryB)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 11, 11, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 11, 20, 0))
                .durationSec(1200)
                .build();
        em.persist(focusB);

        Focus secondA = Focus.builder()
                .library(libraryA)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 11, 12, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 12, 40, 0))
                .durationSec(2400)
                .build();
        em.persist(secondA);

        Focus olderB = Focus.builder()
                .library(libraryB)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 4, 11, 9, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 9, 20, 0))
                .durationSec(1200)
                .build();
        em.persist(olderB);

        em.flush();
        em.clear();

        List<Focus> result = focusRepository.findRecentDistinctBooksByUser(user, PageRequest.of(0, 5));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLibrary().getBook().getTitle()).isEqualTo("A 책");
        assertThat(result.get(1).getLibrary().getBook().getTitle()).isEqualTo("B 책");
        assertThat(result.get(0).getId()).isEqualTo(secondA.getId());
        assertThat(result.get(1).getId()).isEqualTo(focusB.getId());
    }

    @Test
    @DisplayName("조회 구간과 겹치는 완료 및 진행 중 포커스만 조회한다")
    void findOverlappingFocusRanges_반개구간() {
        User owner = persistUser("range-owner");
        User other = persistUser("range-other");
        Book book = persistBook("구간 책", "book/users/1/range.jpg");
        Library ownerLibrary = persistLibrary(owner, book);
        Library otherLibrary = persistLibrary(other, book);
        Theme theme = persistTheme();
        LocalDateTime rangeStart = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 8, 3, 0, 0);

        persistFocus(
                ownerLibrary,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 0),
                rangeStart
        );
        persistFocus(
                ownerLibrary,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 55),
                LocalDateTime.of(2026, 8, 2, 0, 10)
        );
        persistFocus(
                ownerLibrary,
                theme,
                rangeStart,
                LocalDateTime.of(2026, 8, 2, 0, 30)
        );
        persistFocus(
                ownerLibrary,
                theme,
                LocalDateTime.of(2026, 8, 2, 12, 0),
                null
        );
        persistFocus(
                ownerLibrary,
                theme,
                rangeEnd,
                LocalDateTime.of(2026, 8, 3, 0, 30)
        );
        persistFocus(
                otherLibrary,
                theme,
                LocalDateTime.of(2026, 8, 2, 10, 0),
                LocalDateTime.of(2026, 8, 2, 10, 30)
        );

        em.flush();
        em.clear();

        List<FocusRangeStatsDto> result = focusRepository.findOverlappingFocusRanges(
                owner.getId(),
                rangeStart,
                rangeEnd
        );

        assertThat(result).hasSize(3);
        assertThat(result).extracting(FocusRangeStatsDto::getStartedAt)
                .containsExactly(
                        LocalDateTime.of(2026, 8, 1, 23, 55),
                        rangeStart,
                        LocalDateTime.of(2026, 8, 2, 12, 0)
                );
        assertThat(result.get(2).getEndedAt()).isNull();
        assertThat(result).allMatch(row -> row.getBookId().equals(book.getId()));
        assertThat(result).allMatch(row -> row.getCoverImageKey().equals("book/users/1/range.jpg"));
    }

    @Test
    @DisplayName("날짜별 기록은 자정 반개구간과 겹치는 포커스만 조회한다")
    void findByLibraryWithCursorByDate_반개구간() {
        User user = persistUser("daily-owner");
        Book book = persistBook("날짜 책", "book/users/1/daily.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        LocalDate date = LocalDate.of(2026, 8, 2);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        Focus endedAtDayStart = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 0),
                dayStart
        );
        Focus acrossMidnight = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 55),
                LocalDateTime.of(2026, 8, 2, 0, 10)
        );
        Focus startedAtDayStart = persistFocus(
                library,
                theme,
                dayStart,
                LocalDateTime.of(2026, 8, 2, 0, 30)
        );
        Focus inProgress = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 2, 12, 0),
                null
        );
        Focus startedAtNextDay = persistFocus(
                library,
                theme,
                nextDayStart,
                LocalDateTime.of(2026, 8, 3, 0, 30)
        );

        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findByLibraryWithCursorByDate(
                user,
                date,
                nextDayStart,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(Focus::getId)
                .containsExactly(inProgress.getId(), startedAtDayStart.getId(), acrossMidnight.getId())
                .doesNotContain(endedAtDayStart.getId(), startedAtNextDay.getId());
    }

    @Test
    @DisplayName("자정에 끝난 일별 행과 자정에 시작한 일별 행은 각각 하나의 날짜에만 속한다")
    void findByLibraryWithCursorByDate_dailyRowsAtMidnightBelongToOneDateOnly() {
        User user = persistUser("daily-midnight-owner");
        Book book = persistBook("일별 자정 책", "book/users/1/daily-midnight.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        Focus augustFirst = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 1, 23, 55),
                LocalDateTime.of(2026, 8, 2, 0, 0)
        );
        Focus augustSecond = persistFocus(
                library,
                theme,
                LocalDateTime.of(2026, 8, 2, 0, 0),
                LocalDateTime.of(2026, 8, 2, 0, 10)
        );
        em.flush();
        em.clear();

        Slice<Focus> augustFirstRows = focusRepository.findByLibraryWithCursorByDate(
                user,
                LocalDate.of(2026, 8, 1),
                LocalDateTime.of(2026, 8, 3, 0, 0),
                null,
                PageRequest.of(0, 10)
        );
        Slice<Focus> augustSecondRows = focusRepository.findByLibraryWithCursorByDate(
                user,
                LocalDate.of(2026, 8, 2),
                LocalDateTime.of(2026, 8, 3, 0, 0),
                null,
                PageRequest.of(0, 10)
        );

        assertThat(augustFirstRows.getContent()).extracting(Focus::getId).containsExactly(augustFirst.getId());
        assertThat(augustSecondRows.getContent()).extracting(Focus::getId).containsExactly(augustSecond.getId());
    }

    @Test
    void findByLibraryWithCursorByDate_futureWindowReturnsEmpty() {
        User user = persistUser("future-window-owner");
        Book book = persistBook("미래 기록 책", "book/users/1/future.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        LocalDateTime serverNow = LocalDateTime.of(2026, 8, 18, 12, 0);

        persistFocus(library, theme, LocalDateTime.of(2026, 8, 18, 10, 0), null);
        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findByLibraryWithCursorByDate(
                user,
                LocalDate.of(2026, 8, 19),
                serverNow,
                null,
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByLibraryWithCursorByDate_ongoingTodayAndPast() {
        User user = persistUser("ongoing-window-owner");
        Book book = persistBook("진행 중 기록 책", "book/users/1/ongoing.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        LocalDateTime serverNow = LocalDateTime.of(2026, 8, 18, 12, 0);

        Focus pastOngoing = persistFocus(library, theme, LocalDateTime.of(2026, 8, 17, 10, 0), null);
        Focus todayOngoing = persistFocus(library, theme, LocalDateTime.of(2026, 8, 18, 10, 0), null);
        em.flush();
        em.clear();

        Slice<Focus> today = focusRepository.findByLibraryWithCursorByDate(
                user,
                LocalDate.of(2026, 8, 18),
                serverNow,
                null,
                PageRequest.of(0, 10)
        );
        Slice<Focus> past = focusRepository.findByLibraryWithCursorByDate(
                user,
                LocalDate.of(2026, 8, 17),
                serverNow,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(today.getContent()).extracting(Focus::getId).containsExactly(todayOngoing.getId(), pastOngoing.getId());
        assertThat(past.getContent()).extracting(Focus::getId).containsExactly(pastOngoing.getId());
    }

    @Test
    void findByLibraryWithCursorByDate_cursorAppliedAfterServerNowOverlap() {
        User user = persistUser("cursor-window-owner");
        Book book = persistBook("커서 기록 책", "book/users/1/cursor.jpg");
        Library library = persistLibrary(user, book);
        Theme theme = persistTheme();
        LocalDate date = LocalDate.of(2026, 8, 18);
        LocalDateTime serverNow = LocalDateTime.of(2026, 8, 18, 12, 0);

        Focus olderOngoing = persistFocus(library, theme, LocalDateTime.of(2026, 8, 18, 9, 0), null);
        Focus currentOngoing = persistFocus(library, theme, LocalDateTime.of(2026, 8, 18, 10, 0), null);
        Focus futureOngoing = persistFocus(library, theme, LocalDateTime.of(2026, 8, 18, 13, 0), null);
        em.flush();
        em.clear();

        Slice<Focus> result = focusRepository.findByLibraryWithCursorByDate(
                user,
                date,
                serverNow,
                futureOngoing.getId() + 1,
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent()).extracting(Focus::getId).containsExactly(currentOngoing.getId());
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()).extracting(Focus::getId).doesNotContain(futureOngoing.getId());
        assertThat(olderOngoing.getId()).isLessThan(currentOngoing.getId());
    }

    private User persistUser(String suffix) {
        User user = User.builder()
                .email(suffix + "@example.com")
                .nickName(suffix)
                .provider("google")
                .providerId(suffix)
                .role(UserRole.USER)
                .build();
        em.persist(user);
        return user;
    }

    private Book persistBook(String title, String coverImageKey) {
        Book book = Book.builder()
                .title(title)
                .author("테스트 작가")
                .coverImageKey(coverImageKey)
                .build();
        em.persist(book);
        return book;
    }

    private Library persistLibrary(User user, Book book) {
        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(library);
        return library;
    }

    private Theme persistTheme() {
        return themeRepository.save(Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build());
    }

    private Focus persistFocus(
            Library library,
            Theme theme,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(endedAt == null ? 0 : (int) java.time.Duration.between(startedAt, endedAt).getSeconds())
                .build();
        em.persist(focus);
        return focus;
    }
}
