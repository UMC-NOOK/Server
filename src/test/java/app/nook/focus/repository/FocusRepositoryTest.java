package app.nook.focus.repository;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        Focus focus = Focus.builder()
                .library(library)
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

        Focus endedFocus = Focus.builder()
                .library(library)
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

        Focus focus = Focus.builder()
                .library(library)
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

        Focus focus = Focus.builder()
                .library(library)
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

    @Test
    @DisplayName("종료 잠금 조회는 소유자의 포커스만 반환한다")
    void findByIdAndLibraryUserIdForUpdate_소유포커스조회성공() {
        // Given
        User owner = User.builder()
                .email("lock-owner@example.com")
                .nickName("잠금소유자")
                .provider("google")
                .providerId("lock-owner-provider")
                .role(UserRole.USER)
                .build();
        em.persist(owner);
        Book book = Book.builder().title("잠금 테스트 책").author("작가").build();
        em.persist(book);
        Library library = Library.builder().user(owner).book(book).build();
        em.persist(library);
        Focus focus = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 8, 1, 23, 0))
                .durationSec(0)
                .build();
        em.persist(focus);
        em.flush();
        em.clear();

        // When
        Optional<Focus> result = focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), owner.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(focus.getId());
    }

    @Test
    @DisplayName("종료 잠금 조회는 다른 유저의 포커스를 숨긴다")
    void findByIdAndLibraryUserIdForUpdate_다른유저는조회실패() {
        // Given
        User owner = User.builder()
                .email("lock-owner-foreign@example.com")
                .nickName("잠금소유자")
                .provider("google")
                .providerId("lock-owner-foreign-provider")
                .role(UserRole.USER)
                .build();
        User foreignUser = User.builder()
                .email("lock-foreign@example.com")
                .nickName("잠금외부사용자")
                .provider("google")
                .providerId("lock-foreign-provider")
                .role(UserRole.USER)
                .build();
        em.persist(owner);
        em.persist(foreignUser);
        Book book = Book.builder().title("잠금 외부 테스트 책").author("작가").build();
        em.persist(book);
        Library library = Library.builder().user(owner).book(book).build();
        em.persist(library);
        Focus focus = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 8, 1, 23, 0))
                .durationSec(0)
                .build();
        em.persist(focus);
        em.flush();
        em.clear();

        // When
        Optional<Focus> result = focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), foreignUser.getId());

        // Then
        assertThat(result).isEmpty();
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

        Focus older = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 4, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 1, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(older);

        Focus newer = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 4, 2, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 2, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(newer);

        // endedAt == null 인 진행 중 포커스 (결과에 포함되면 안 됨)
        Focus inProgress = Focus.builder()
                .library(library)
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

        Focus focus1 = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 5, 1, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(focus1);

        Focus focus2 = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 5, 2, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 5, 2, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(focus2);

        Focus focus3 = Focus.builder()
                .library(library)
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

        for (int i = 1; i <= 3; i++) {
            em.persist(Focus.builder()
                    .library(library)
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

        em.persist(Focus.builder()
                .library(ownerLibrary)
                .startedAt(LocalDateTime.of(2026, 7, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 7, 1, 10, 30, 0))
                .durationSec(1800)
                .build());

        em.persist(Focus.builder()
                .library(otherLibrary)
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

        Focus firstA = Focus.builder()
                .library(libraryA)
                .startedAt(LocalDateTime.of(2026, 4, 11, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 10, 30, 0))
                .durationSec(1800)
                .build();
        em.persist(firstA);

        Focus focusB = Focus.builder()
                .library(libraryB)
                .startedAt(LocalDateTime.of(2026, 4, 11, 11, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 11, 20, 0))
                .durationSec(1200)
                .build();
        em.persist(focusB);

        Focus secondA = Focus.builder()
                .library(libraryA)
                .startedAt(LocalDateTime.of(2026, 4, 11, 12, 0, 0))
                .endedAt(LocalDateTime.of(2026, 4, 11, 12, 40, 0))
                .durationSec(2400)
                .build();
        em.persist(secondA);

        Focus olderB = Focus.builder()
                .library(libraryB)
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
    @DisplayName("유저의 날짜별 도서 포커스 시간을 합산하고 조회 범위와 소유자를 분리한다")
    void findMonthlyFocusStats_날짜별도서합산및범위격리() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 8, 10);
        User owner = User.builder()
                .email("monthly-stats-owner@example.com")
                .nickName("통계소유자")
                .provider("google")
                .providerId("monthly-stats-owner-provider")
                .role(UserRole.USER)
                .build();
        User other = User.builder()
                .email("monthly-stats-other@example.com")
                .nickName("다른통계유저")
                .provider("google")
                .providerId("monthly-stats-other-provider")
                .role(UserRole.USER)
                .build();
        em.persist(owner);
        em.persist(other);

        Book firstBook = Book.builder().title("첫 번째 책").author("작가1").build();
        Book secondBook = Book.builder().title("두 번째 책").author("작가2").build();
        em.persist(firstBook);
        em.persist(secondBook);

        Library ownerFirstLibrary = Library.builder().user(owner).book(firstBook).build();
        Library ownerSecondLibrary = Library.builder().user(owner).book(secondBook).build();
        Library otherFirstLibrary = Library.builder().user(other).book(firstBook).build();
        em.persist(ownerFirstLibrary);
        em.persist(ownerSecondLibrary);
        em.persist(otherFirstLibrary);

        em.persist(Focus.builder()
                .library(ownerFirstLibrary)
                .startedAt(targetDate.atTime(10, 0))
                .endedAt(targetDate.atTime(10, 20))
                .durationSec(1_200)
                .build());
        em.persist(Focus.builder()
                .library(ownerFirstLibrary)
                .startedAt(targetDate.atTime(11, 0))
                .endedAt(targetDate.atTime(11, 30))
                .durationSec(1_800)
                .build());
        em.persist(Focus.builder()
                .library(ownerFirstLibrary)
                .startedAt(targetDate.atTime(12, 0))
                .endedAt(null)
                .durationSec(0)
                .build());
        em.persist(Focus.builder()
                .library(ownerSecondLibrary)
                .startedAt(targetDate.atTime(13, 0))
                .endedAt(targetDate.atTime(13, 15))
                .durationSec(900)
                .build());
        em.persist(Focus.builder()
                .library(ownerFirstLibrary)
                .startedAt(targetDate.minusDays(1).atTime(23, 0))
                .endedAt(targetDate.minusDays(1).atTime(23, 10))
                .durationSec(600)
                .build());
        em.persist(Focus.builder()
                .library(ownerFirstLibrary)
                .startedAt(targetDate.plusDays(1).atTime(0, 10))
                .endedAt(targetDate.plusDays(1).atTime(0, 25))
                .durationSec(900)
                .build());
        em.persist(Focus.builder()
                .library(otherFirstLibrary)
                .startedAt(targetDate.atTime(14, 0))
                .endedAt(targetDate.atTime(16, 0))
                .durationSec(7_200)
                .build());

        em.flush();
        em.clear();

        // when
        List<MonthlyFocusStatsDto> result = focusRepository.findMonthlyFocusStats(
                owner.getId(),
                targetDate,
                targetDate.plusDays(1)
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(row -> assertThat(row.getFocusDate()).isEqualTo(targetDate));

        Map<Long, Long> totalsByBook = result.stream()
                .collect(Collectors.toMap(
                        MonthlyFocusStatsDto::getBookId,
                        MonthlyFocusStatsDto::getTotalSec
                ));
        assertThat(totalsByBook).containsExactlyInAnyOrderEntriesOf(Map.of(
                firstBook.getId(), 3_000L,
                secondBook.getId(), 900L
        ));
    }
}
