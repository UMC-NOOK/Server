package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class BookViewHistoryRepositoryTest extends AbstractPostgresContainerTests {

    @Autowired
    private BookViewHistoryRepository bookViewHistoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Book firstBook;
    private Book secondBook;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(createUser("test@example.com", "provider-1"));
        firstBook = bookRepository.save(createBook("첫 번째 책"));
        secondBook = bookRepository.save(createBook("두 번째 책"));
    }

    @Test
    @DisplayName("도서 조회 이력 저장 성공")
    void 도서조회이력_저장_성공() {
        // given
        BookViewHistory history = createBookViewHistory(firstBook);

        // when
        BookViewHistory saved = bookViewHistoryRepository.save(history);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getBook()).isEqualTo(firstBook);
    }

    @Test
    @DisplayName("기존 도서 조회 이력 확인")
    void findExisting_존재() {
        // given
        bookViewHistoryRepository.save(createBookViewHistory(firstBook));

        // when
        Optional<BookViewHistory> found = bookViewHistoryRepository.findExisting(testUser, firstBook);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getBook()).isEqualTo(firstBook);
    }

    @Test
    @DisplayName("최근 조회 이력은 사용자별 최신 5건을 동률 시 ID 내림차순과 초기화된 도서로 조회")
    void findAllRecent_사용자별_최신5건_동률시_ID내림차순_도서FetchJoin() {
        // given
        List<BookViewHistory> userHistories = new ArrayList<>();
        for (int index = 1; index <= 7; index++) {
            Book book = bookRepository.save(createBook("사용자 책 " + index));
            userHistories.add(bookViewHistoryRepository.save(createBookViewHistory(book)));
        }
        User anotherUser = userRepository.save(createUser("another@example.com", "provider-2"));
        BookViewHistory anotherUserHistory = bookViewHistoryRepository.save(BookViewHistory.builder()
                .user(anotherUser)
                .book(bookRepository.save(createBook("다른 사용자 책")))
                .build());

        entityManager.flush();
        LocalDateTime sameModifiedDate = LocalDateTime.of(2026, 8, 20, 12, 0);
        entityManager.createNativeQuery("UPDATE book_view_history SET modified_date = :modifiedDate")
                .setParameter("modifiedDate", Timestamp.valueOf(sameModifiedDate))
                .executeUpdate();
        entityManager.clear();

        List<Long> expectedHistoryIds = userHistories.stream()
                .map(BookViewHistory::getId)
                .sorted(Comparator.reverseOrder())
                .toList();

        // when
        List<BookViewHistory> histories = bookViewHistoryRepository.findAllRecent(testUser, PageRequest.of(0, 5));

        // then
        assertThat(histories).hasSize(5);
        assertThat(histories)
                .extracting(BookViewHistory::getId)
                .containsExactlyElementsOf(expectedHistoryIds.subList(0, 5));
        assertThat(histories).allSatisfy(history -> {
            assertThat(history.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(Hibernate.isInitialized(history.getBook())).isTrue();
        });
        assertThat(histories)
                .extracting(BookViewHistory::getId)
                .doesNotContain(anotherUserHistory.getId());
        System.out.printf(
                "DATA_SURFACE recent-history historyIds=%s bookIds=%s size=%d otherUserHistoryExcluded=%s bookAssociationsInitialized=%s%n",
                histories.stream().map(BookViewHistory::getId).toList(),
                histories.stream().map(history -> history.getBook().getId()).toList(),
                histories.size(),
                histories.stream().noneMatch(history -> history.getId().equals(anotherUserHistory.getId())),
                histories.stream().allMatch(history -> Hibernate.isInitialized(history.getBook())));
    }

    private BookViewHistory createBookViewHistory(Book book) {
        return BookViewHistory.builder()
                .user(testUser)
                .book(book)
                .build();
    }

    private User createUser(String email, String providerId) {
        return User.builder()
                .email(email)
                .nickName("테스터")
                .provider("google")
                .providerId(providerId)
                .role(UserRole.USER)
                .build();
    }

    private Book createBook(String title) {
        return Book.builder()
                .title(title)
                .build();
    }
}
