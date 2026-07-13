package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.book.repository.BookViewHistoryRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookViewHistoryServiceTest {

    @Mock
    private BookViewHistoryRepository bookViewHistoryRepository;

    @InjectMocks
    private BookViewHistoryService bookViewHistoryService;

    @Captor
    private ArgumentCaptor<BookViewHistory> historyCaptor;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-1")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testBook = Book.builder()
                .title("테스트 책")
                .build();
        ReflectionTestUtils.setField(testBook, "id", 10L);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 성공 - 새로운 도서")
    void saveBookView_새로운도서_성공() {
        // given
        given(bookViewHistoryRepository.findExisting(testUser, testBook))
                .willReturn(Optional.empty());
        given(bookViewHistoryRepository.findAllRecentForUpdate(testUser))
                .willReturn(new ArrayList<>());

        // when
        bookViewHistoryService.saveBookView(testUser, testBook);

        // then
        verify(bookViewHistoryRepository).save(historyCaptor.capture());
        BookViewHistory saved = historyCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getBook()).isEqualTo(testBook);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 중복 도서 삭제 후 새로 저장")
    void saveBookView_중복도서_삭제후저장() {
        // given
        BookViewHistory existingHistory = createBookViewHistory(testBook);

        given(bookViewHistoryRepository.findExisting(testUser, testBook))
                .willReturn(Optional.of(existingHistory));
        given(bookViewHistoryRepository.findAllRecentForUpdate(testUser))
                .willReturn(new ArrayList<>());

        // when
        bookViewHistoryService.saveBookView(testUser, testBook);

        // then
        verify(bookViewHistoryRepository).delete(existingHistory);
        verify(bookViewHistoryRepository).flush();
        verify(bookViewHistoryRepository).save(historyCaptor.capture());

        BookViewHistory saved = historyCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getBook()).isEqualTo(testBook);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 10개 초과 시 가장 오래된 기록 삭제")
    void saveBookView_10개초과_가장오래된기록삭제() {
        // given
        List<BookViewHistory> histories = createMultipleHistories(10);
        BookViewHistory oldest = histories.get(9);

        given(bookViewHistoryRepository.findExisting(testUser, testBook))
                .willReturn(Optional.empty());
        given(bookViewHistoryRepository.findAllRecentForUpdate(testUser))
                .willReturn(histories);

        // when
        bookViewHistoryService.saveBookView(testUser, testBook);

        // then
        verify(bookViewHistoryRepository).delete(oldest);
        verify(bookViewHistoryRepository).save(any(BookViewHistory.class));
    }

    private BookViewHistory createBookViewHistory(Book book) {
        return BookViewHistory.builder()
                .user(testUser)
                .book(book)
                .build();
    }

    private List<BookViewHistory> createMultipleHistories(int count) {
        List<BookViewHistory> histories = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Book book = Book.builder()
                    .title("테스트 책 " + i)
                    .build();
            ReflectionTestUtils.setField(book, "id", (long) i);
            histories.add(createBookViewHistory(book));
        }

        return histories;
    }
}
