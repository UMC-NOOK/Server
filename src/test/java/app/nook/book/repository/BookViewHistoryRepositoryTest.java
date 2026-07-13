package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.global.config.QueryDslConfig;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class BookViewHistoryRepositoryTest {

    @Autowired
    private BookViewHistoryRepository bookViewHistoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("최근 조회 이력을 최신순으로 조회")
    void findAllRecent_최신순_조회() {
        // given
        BookViewHistory firstHistory = bookViewHistoryRepository.save(createBookViewHistory(firstBook));
        BookViewHistory secondHistory = bookViewHistoryRepository.save(createBookViewHistory(secondBook));

        // when
        List<BookViewHistory> histories = bookViewHistoryRepository.findAllRecent(testUser);

        // then
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getId()).isEqualTo(secondHistory.getId());
        assertThat(histories.get(1).getId()).isEqualTo(firstHistory.getId());
    }

    @Test
    @DisplayName("다른 유저의 도서 조회 이력은 조회되지 않음")
    void 다른유저_도서조회이력_분리() {
        // given
        User anotherUser = userRepository.save(createUser("another@example.com", "provider-2"));
        bookViewHistoryRepository.save(createBookViewHistory(firstBook));
        bookViewHistoryRepository.save(BookViewHistory.builder()
                .user(anotherUser)
                .book(secondBook)
                .build());

        // when
        List<BookViewHistory> testUserHistories = bookViewHistoryRepository.findAllRecent(testUser);
        List<BookViewHistory> anotherUserHistories = bookViewHistoryRepository.findAllRecent(anotherUser);

        // then
        assertThat(testUserHistories).hasSize(1);
        assertThat(testUserHistories.get(0).getBook()).isEqualTo(firstBook);
        assertThat(anotherUserHistories).hasSize(1);
        assertThat(anotherUserHistories.get(0).getBook()).isEqualTo(secondBook);
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
