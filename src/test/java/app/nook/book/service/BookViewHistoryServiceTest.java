package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.repository.BookViewHistoryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookViewHistoryServiceTest {

    @Mock
    private BookViewHistoryRepository bookViewHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

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
        given(userRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(testUser));
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

        InOrder inOrder = inOrder(userRepository, bookViewHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(1L);
        inOrder.verify(bookViewHistoryRepository).findExisting(testUser, testBook);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 중복 도서 삭제 후 새로 저장")
    void saveBookView_중복도서_삭제후저장() {
        // given
        BookViewHistory existingHistory = createBookViewHistory(testBook);

        given(userRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(testUser));
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

        InOrder inOrder = inOrder(userRepository, bookViewHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(1L);
        inOrder.verify(bookViewHistoryRepository).findExisting(testUser, testBook);
        inOrder.verify(bookViewHistoryRepository).delete(existingHistory);
        inOrder.verify(bookViewHistoryRepository).flush();
        inOrder.verify(bookViewHistoryRepository).findAllRecentForUpdate(testUser);
        inOrder.verify(bookViewHistoryRepository).save(any(BookViewHistory.class));
    }

    @ParameterizedTest(name = "existing rows: {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("도서 조회 이력 저장 - 0~4개는 보존 이력을 삭제하지 않음")
    void saveBookView_4개이하_보존이력삭제안함(int existingCount) {
        List<BookViewHistory> histories = createMultipleHistories(existingCount);

        given(userRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(testUser));
        given(bookViewHistoryRepository.findExisting(testUser, testBook))
                .willReturn(Optional.empty());
        given(bookViewHistoryRepository.findAllRecentForUpdate(testUser))
                .willReturn(histories);

        bookViewHistoryService.saveBookView(testUser, testBook);

        verify(bookViewHistoryRepository, never()).delete(any(BookViewHistory.class));
        verify(bookViewHistoryRepository).save(any(BookViewHistory.class));
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 5개면 가장 오래된 1개 삭제")
    void saveBookView_5개_가장오래된1개삭제() {
        assertRetentionDeletes(5, 4);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 레거시 6개면 가장 오래된 2개 삭제")
    void saveBookView_레거시6개_가장오래된2개삭제() {
        assertRetentionDeletes(6, 4, 5);
    }

    @Test
    @DisplayName("도서 조회 이력 저장 - 레거시 10개면 가장 오래된 6개 삭제")
    void saveBookView_레거시10개_가장오래된6개삭제() {
        assertRetentionDeletes(10, 4, 5, 6, 7, 8, 9);
    }

    @Test
    @DisplayName("최근 조회 도서 조회 - 저장소 순서대로 다섯 권을 정확한 DTO로 변환한다")
    void getRecentlyViewedBooks_다섯권_저장소순서와DTO필드를보존한다() {
        // given
        List<BookViewHistory> histories = List.of(
                createBookViewHistory(createBook(101L, "첫 번째", "저자 1", "book/users/1/first.png")),
                createBookViewHistory(createBook(102L, "두 번째", "저자 2", "book/users/1/second.png")),
                createBookViewHistory(createBook(103L, "세 번째", "저자 3", "book/users/1/third.png")),
                createBookViewHistory(createBook(104L, "네 번째", "저자 4", "book/users/1/fourth.png")),
                createBookViewHistory(createBook(105L, "다섯 번째", "저자 5", "book/users/1/fifth.png"))
        );
        given(bookViewHistoryRepository.findAllRecent(testUser, PageRequest.of(0, 5)))
                .willReturn(histories);
        given(presignedUrlService.resolveImageUrl(1L, "book/users/1/first.png"))
                .willReturn("https://cdn.example.com/first.png");
        given(presignedUrlService.resolveImageUrl(1L, "book/users/1/second.png"))
                .willReturn("https://cdn.example.com/second.png");
        given(presignedUrlService.resolveImageUrl(1L, "book/users/1/third.png"))
                .willReturn("https://cdn.example.com/third.png");
        given(presignedUrlService.resolveImageUrl(1L, "book/users/1/fourth.png"))
                .willReturn("https://cdn.example.com/fourth.png");
        given(presignedUrlService.resolveImageUrl(1L, "book/users/1/fifth.png"))
                .willReturn("https://cdn.example.com/fifth.png");

        // when
        List<BookResponseDto.RecentlyViewedBookDto> result = bookViewHistoryService.getRecentlyViewedBooks(testUser);

        // then
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::bookId)
                .containsExactly(101L, 102L, 103L, 104L, 105L);
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::title)
                .containsExactly("첫 번째", "두 번째", "세 번째", "네 번째", "다섯 번째");
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::author)
                .containsExactly("저자 1", "저자 2", "저자 3", "저자 4", "저자 5");
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::coverImageUrl)
                .containsExactly(
                        "https://cdn.example.com/first.png",
                        "https://cdn.example.com/second.png",
                        "https://cdn.example.com/third.png",
                        "https://cdn.example.com/fourth.png",
                        "https://cdn.example.com/fifth.png"
                );
        verify(bookViewHistoryRepository).findAllRecent(testUser, PageRequest.of(0, 5));
        verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/first.png");
        verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/second.png");
        verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/third.png");
        verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/fourth.png");
        verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/fifth.png");
    }

    @Test
    @DisplayName("최근 조회 도서 조회 - 외부 URL과 null 및 빈 표지를 resolver 결과 그대로 반환한다")
    void getRecentlyViewedBooks_외부NullBlank표지_resolver결과를보존한다() {
        // given
        String externalUrl = "https://image.aladin.co.kr/product/cover.jpg";
        List<BookViewHistory> histories = List.of(
                createBookViewHistory(createBook(201L, "외부 표지", "저자", externalUrl)),
                createBookViewHistory(createBook(202L, "null 표지", "저자", null)),
                createBookViewHistory(createBook(203L, "빈 표지", "저자", "")),
                createBookViewHistory(createBook(204L, "공백 표지", "저자", " "))
        );
        given(bookViewHistoryRepository.findAllRecent(testUser, PageRequest.of(0, 5)))
                .willReturn(histories);
        given(presignedUrlService.resolveImageUrl(1L, externalUrl)).willReturn(externalUrl);
        given(presignedUrlService.resolveImageUrl(1L, null)).willReturn(null);
        given(presignedUrlService.resolveImageUrl(1L, "")).willReturn("");
        given(presignedUrlService.resolveImageUrl(1L, " ")).willReturn(" ");

        // when
        List<BookResponseDto.RecentlyViewedBookDto> result = bookViewHistoryService.getRecentlyViewedBooks(testUser);

        // then
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::bookId)
                .containsExactly(201L, 202L, 203L, 204L);
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::coverImageUrl)
                .containsExactly(externalUrl, null, "", " ");
        verify(presignedUrlService).resolveImageUrl(1L, externalUrl);
        verify(presignedUrlService).resolveImageUrl(1L, null);
        verify(presignedUrlService).resolveImageUrl(1L, "");
        verify(presignedUrlService).resolveImageUrl(1L, " ");
    }

    @Test
    @DisplayName("최근 조회 도서 조회 - 이력이 없으면 비어 있는 목록을 반환한다")
    void getRecentlyViewedBooks_이력없음_빈목록을반환한다() {
        // given
        given(bookViewHistoryRepository.findAllRecent(testUser, PageRequest.of(0, 5)))
                .willReturn(List.of());

        // when
        List<BookResponseDto.RecentlyViewedBookDto> result = bookViewHistoryService.getRecentlyViewedBooks(testUser);

        // then
        assertThat(result).isNotNull().isEmpty();
        verify(bookViewHistoryRepository).findAllRecent(testUser, PageRequest.of(0, 5));
        verify(presignedUrlService, never()).resolveImageUrl(any(), any());
    }

    @Test
    @DisplayName("최근 조회 도서 조회 - 저장소가 여섯 건을 제공해도 첫 다섯 DTO만 반환한다")
    void getRecentlyViewedBooks_저장소에6건_첫다섯DTO만반환한다() {
        // given
        List<BookViewHistory> histories = List.of(
                createBookViewHistory(createBook(301L, "첫 번째", "저자", null)),
                createBookViewHistory(createBook(302L, "두 번째", "저자", null)),
                createBookViewHistory(createBook(303L, "세 번째", "저자", null)),
                createBookViewHistory(createBook(304L, "네 번째", "저자", null)),
                createBookViewHistory(createBook(305L, "다섯 번째", "저자", null)),
                createBookViewHistory(createBook(306L, "여섯 번째", "저자", null))
        );
        given(bookViewHistoryRepository.findAllRecent(testUser, PageRequest.of(0, 5)))
                .willReturn(histories);

        // when
        List<BookResponseDto.RecentlyViewedBookDto> result = bookViewHistoryService.getRecentlyViewedBooks(testUser);

        // then
        assertThat(result).extracting(BookResponseDto.RecentlyViewedBookDto::bookId)
                .containsExactly(301L, 302L, 303L, 304L, 305L);
        verify(bookViewHistoryRepository).findAllRecent(testUser, PageRequest.of(0, 5));
    }

    private void assertRetentionDeletes(int existingCount, int... deletedIndexes) {
        List<BookViewHistory> histories = createMultipleHistories(existingCount);

        given(userRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(testUser));
        given(bookViewHistoryRepository.findExisting(testUser, testBook))
                .willReturn(Optional.empty());
        given(bookViewHistoryRepository.findAllRecentForUpdate(testUser))
                .willReturn(histories);

        bookViewHistoryService.saveBookView(testUser, testBook);

        List<BookViewHistory> expectedDeletes = new ArrayList<>();
        for (int deletedIndex : deletedIndexes) {
            expectedDeletes.add(histories.get(deletedIndex));
        }

        ArgumentCaptor<BookViewHistory> deletedHistoryCaptor = ArgumentCaptor.forClass(BookViewHistory.class);
        verify(bookViewHistoryRepository, org.mockito.Mockito.times(deletedIndexes.length))
                .delete(deletedHistoryCaptor.capture());
        assertThat(deletedHistoryCaptor.getAllValues()).containsExactlyElementsOf(expectedDeletes);
        verify(bookViewHistoryRepository).save(any(BookViewHistory.class));
    }

    private BookViewHistory createBookViewHistory(Book book) {
        return BookViewHistory.builder()
                .user(testUser)
                .book(book)
                .build();
    }

    private Book createBook(Long id, String title, String author, String coverImageKey) {
        Book book = Book.builder()
                .title(title)
                .author(author)
                .coverImageKey(coverImageKey)
                .build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
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
