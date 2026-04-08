package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LibraryService 테스트")
class LibraryServiceTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private TimelineCommandService timelineCommandService;

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        lenient().when(presignedUrlService.resolveImageUrl(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private User user() {
        return User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
    }

    @Nested
    @DisplayName("서재 책 등록")
    class Save {

        @Test
        @DisplayName("성공")
        void 책_등록_성공() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();
            ReflectionTestUtils.setField(book, "id", 1L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);
            given(libraryRepository.save(any(Library.class))).willAnswer(invocation -> {
                Library saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                ReflectionTestUtils.setField(saved, "createdDate", LocalDateTime.now());
                return saved;
            });

            libraryService.save(user, 1L);

            verify(timelineCommandService).appendRegister(any());
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void save_도서없음_예외() {
            User user = user();
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryService.save(user, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 등록된 도서면 예외를 던진다")
        void save_이미등록_예외() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            Library library = Library.builder()
                    .user(user)
                    .book(book)
                    .build();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);

            CustomException ex = assertThrows(CustomException.class, () -> libraryService.save(user, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_ALREADY_EXIST);
        }
    }

    @Nested
    @DisplayName("서재 책 삭제")
    class DeleteById {

        @Test
        @DisplayName("성공")
        void deleteById_성공() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            Library library = Library.builder()
                    .user(user)
                    .book(book)
                    .build();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(focusRepository.findDistinctFocusDatesByLibraryAndUser(any(), any())).willReturn(List.of());

            libraryService.deleteById(user, 1L);

            verify(libraryRepository).delete(library);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void deleteById_서재없음_예외() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);

            CustomException ex = assertThrows(CustomException.class, () -> libraryService.deleteById(user, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("서재 독서 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("성공")
        void changeStatus_성공() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            Library library = Library.builder()
                    .user(user)
                    .book(book)
                    .build();

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);

            libraryService.changeStatus(user, request);

            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            verify(timelineCommandService).appendStatusChanged(any(), any());
        }

        @Test
        @DisplayName("중복 상태 변경 요청이면 예외를 던진다")
        void changeStatus_중복상태_예외() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            Library library = Library.builder()
                    .user(user)
                    .book(book)
                    .build();
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);

            CustomException ex = assertThrows(CustomException.class, () -> libraryService.changeStatus(user, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_STATUS_INVALID);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void changeStatus_서재없음_예외() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .build();

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);

            CustomException ex = assertThrows(CustomException.class, () -> libraryService.changeStatus(user, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("책 상태별 조회")
    class ViewBooksByStatus {

        @Test
        @DisplayName("첫 조회면 전체 개수를 포함한다")
        void viewBooksByStatus_첫조회_전체개수포함() {
            User user = user();

            Book book1 = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서1")
                    .author("작가1")
                    .coverImageKey("https://example.com/cover1.jpg")
                    .build();
            ReflectionTestUtils.setField(book1, "id", 1L);

            Book book2 = Book.builder()
                    .isbn13("1234567890124")
                    .title("테스트 도서2")
                    .author("작가2")
                    .coverImageKey("https://example.com/cover2.jpg")
                    .build();
            ReflectionTestUtils.setField(book2, "id", 2L);

            Library library1 = Library.builder().user(user).book(book1).build();
            ReflectionTestUtils.setField(library1, "id", 10L);
            ReflectionTestUtils.setField(library1, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library1, "startedAt", LocalDate.of(2025, 1, 1));

            Library library2 = Library.builder().user(user).book(book2).build();
            ReflectionTestUtils.setField(library2, "id", 9L);
            ReflectionTestUtils.setField(library2, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library2, "startedAt", LocalDate.of(2025, 1, 2));

            int size = 1;
            Slice<Library> slice = new SliceImpl<>(List.of(library1, library2), PageRequest.of(0, size + 1), true);

            given(libraryRepository.findByStatusWithCursor(any(), any(), any(), any())).willReturn(slice);
            given(libraryRepository.countByUserAndReadingStatus(any(), any())).willReturn(10L);

            LibraryViewDto.StatusBookResponseDto response =
                    libraryService.viewBooksByStatus(user, ReadingStatus.READING, null, size);

            assertThat(response.readingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(response.totalBookNum()).isEqualTo(10);
            assertThat(response.bookItems().getItems()).hasSize(1);
            verify(libraryRepository).countByUserAndReadingStatus(user, ReadingStatus.READING);
        }

        @Test
        @DisplayName("커서 조회면 전체 개수를 포함하지 않는다")
        void viewBooksByStatus_커서조회_전체개수미포함() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("테스트 도서")
                    .author("작가")
                    .coverImageKey("https://example.com/cover.jpg")
                    .build();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = Library.builder().user(user).book(book).build();
            ReflectionTestUtils.setField(library, "id", 8L);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library, "startedAt", LocalDate.of(2025, 1, 1));

            int size = 1;
            Slice<Library> slice = new SliceImpl<>(List.of(library), PageRequest.of(0, size + 1), false);

            given(libraryRepository.findByStatusWithCursor(any(), any(), anyLong(), any())).willReturn(slice);

            LibraryViewDto.StatusBookResponseDto response =
                    libraryService.viewBooksByStatus(user, ReadingStatus.READING, 100L, size);

            assertThat(response.totalBookNum()).isZero();
            verify(libraryRepository, never()).countByUserAndReadingStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("책 개수 조회")
    class CountBooks {
        @Test
        @DisplayName("정상")
        void countBooks_정상() {
            User user = user();
            given(libraryRepository.countByUser(user)).willReturn(7);

            LibraryViewDto.BookCountResponseDto result = libraryService.countBooks(user);

            assertThat(result.totalBookNum()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("findOwnedIsbns")
    class FindOwnedIsbns {
        @Test
        @DisplayName("정상")
        void findOwnedIsbns_정상() {
            Long userId = 1L;
            List<String> isbns = List.of("978123", "978456");
            Set<String> owned = Set.of("978123");

            given(libraryRepository.findIsbnsByUserIdAndIsbnIn(userId, isbns)).willReturn(owned);

            Set<String> result = libraryService.findOwnedIsbns(userId, isbns);

            assertThat(result).containsExactly("978123");
        }
    }

    @Nested
    @DisplayName("서재 책 검색")
    class SearchBooksInLibrary {
        @Test
        @DisplayName("키워드 이스케이프를 적용해 조회한다")
        void searchBooksInLibrary_키워드_이스케이프_정상() {
            Long userId = 1L;
            String rawKeyword = "A\\B%_C";
            String escapedKeyword = "A\\\\B\\%\\_C";
            int page = 0;
            int size = 10;

            Page<Library> expected = new PageImpl<>(List.of());
            given(libraryRepository.searchByUserIdAndKeyword(eq(userId), eq(escapedKeyword), any(PageRequest.class)))
                    .willReturn(expected);

            Page<Library> result = libraryService.searchBooksInLibrary(userId, rawKeyword, page, size);

            assertThat(result).isEqualTo(expected);
            verify(libraryRepository).searchByUserIdAndKeyword(eq(userId), eq(escapedKeyword), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("포커스 기록 조회")
    class ViewFocusRecordByDate {

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext=true와 nextCursor를 반환한다")
        void viewFocusRecordByDate_다음페이지_존재() {
            User user = user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book1 = Book.builder()
                    .isbn13("1234567890123")
                    .title("도서1")
                    .author("작가1")
                    .coverImageKey("cover1")
                    .build();
            ReflectionTestUtils.setField(book1, "id", 1L);
            Book book2 = Book.builder()
                    .isbn13("1234567890124")
                    .title("도서2")
                    .author("작가2")
                    .coverImageKey("cover2")
                    .build();
            ReflectionTestUtils.setField(book2, "id", 2L);

            Library library1 = Library.builder().user(user).book(book1).build();
            Library library2 = Library.builder().user(user).book(book2).build();

            Focus focus1 = new Focus();
            ReflectionTestUtils.setField(focus1, "id", 30L);
            ReflectionTestUtils.setField(focus1, "library", library1);
            ReflectionTestUtils.setField(focus1, "durationSec", 120);

            Focus focus2 = new Focus();
            ReflectionTestUtils.setField(focus2, "id", 29L);
            ReflectionTestUtils.setField(focus2, "library", library2);
            ReflectionTestUtils.setField(focus2, "durationSec", null);

            int size = 1;
            Slice<Focus> slice = new SliceImpl<>(List.of(focus1, focus2), PageRequest.of(0, size + 1), true);
            given(focusRepository.findByLibraryWithCursorByDate(eq(user), eq(date), isNull(), any(PageRequest.class)))
                    .willReturn(slice);

            var result = libraryService.viewFocusRecordByDate(user, date, null, size);

            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(30L);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).focusSec()).isEqualTo(120);
        }

        @Test
        @DisplayName("마지막 페이지면 hasNext=false이며 null duration은 0으로 반환한다")
        void viewFocusRecordByDate_마지막페이지_및_null_duration_처리() {
            User user = user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("도서1")
                    .author("작가1")
                    .coverImageKey("cover1")
                    .build();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = Library.builder().user(user).book(book).build();

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 10L);
            ReflectionTestUtils.setField(focus, "library", library);
            ReflectionTestUtils.setField(focus, "durationSec", null);

            int size = 2;
            Slice<Focus> slice = new SliceImpl<>(List.of(focus), PageRequest.of(0, size + 1), false);
            given(focusRepository.findByLibraryWithCursorByDate(eq(user), eq(date), eq(100L), any(PageRequest.class)))
                    .willReturn(slice);

            var result = libraryService.viewFocusRecordByDate(user, date, 100L, size);

            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).focusSec()).isZero();
        }
    }

    @Nested
    @DisplayName("최근 포커스 조회")
    class ViewRecentFocus {

        @Test
        @DisplayName("최근 포커스가 있으면 bookId/title/page를 반환하고 page 0은 null 처리한다")
        void viewRecentFocus_성공_page_null_처리() {
            User user = user();

            Book book = Book.builder()
                    .isbn13("1234567890123")
                    .title("최근 도서")
                    .author("작가")
                    .coverImageKey("cover")
                    .build();
            ReflectionTestUtils.setField(book, "id", 11L);

            Library library = Library.builder().user(user).book(book).build();
            ReflectionTestUtils.setField(library, "page", 0);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 99L);
            ReflectionTestUtils.setField(focus, "library", library);

            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of(focus));

            LibraryViewDto.RecentFocusResponseDto result = libraryService.viewRecentFocus(user);

            assertThat(result).isNotNull();
            assertThat(result.bookId()).isEqualTo(11L);
            assertThat(result.title()).isEqualTo("최근 도서");
            assertThat(result.page()).isNull();
        }

        @Test
        @DisplayName("최근 포커스가 없으면 null을 반환한다")
        void viewRecentFocus_데이터없음_null반환() {
            User user = user();
            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of());

            LibraryViewDto.RecentFocusResponseDto result = libraryService.viewRecentFocus(user);

            assertThat(result).isNull();
        }
    }
}
