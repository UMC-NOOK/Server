package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.BookFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Library 서비스 테스트")
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LibraryCommandService libraryCommandService;

    @InjectMocks
    private LibraryQueryService libraryQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(presignedUrlService.resolveImageUrl(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Nested
    @DisplayName("서재 책 등록")
    class Save {

        @Test
        @DisplayName("성공")
        void 책_등록_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());
            given(libraryRepository.saveAndFlush(any(Library.class))).willAnswer(invocation -> {
                Library saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                ReflectionTestUtils.setField(saved, "createdDate", LocalDateTime.now());
                return saved;
            });

            libraryCommandService.registerBook(1L, 1L);

            verify(timelineCommandService).appendRegister(any());
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void save_도서없음_예외() {
            User user = UserFixture.user();
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.registerBook(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 등록된 도서면 예외를 던진다")
        void save_이미등록_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.registerBook(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_ALREADY_EXIST);
        }
    }

    @Nested
    @DisplayName("서재 책 삭제")
    class DeleteById {

        @Test
        @DisplayName("성공")
        void deleteById_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(libraryRepository).delete(library);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void deleteById_서재없음_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.deleteByBookId(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void deleteById_도서없음_예외() {
            User user = UserFixture.user();
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.deleteByBookId(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제 시 사용자 기준 월별 캐시 무효화 이벤트를 발행한다")
        void deleteById_캐시무효화_이벤트발행() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(1L)
            ));
        }

        @Test
        @DisplayName("삭제 시 포커스 조회 없이 삭제를 수행한다")
        void deleteById_포커스조회없이_삭제() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(focusRepository, never()).findDistinctFocusDatesByLibraryAndUser(any(), any());
        }
    }

    @Nested
    @DisplayName("서재 독서 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("성공")
        void changeStatus_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.changeReadingStatus(1L, request);

            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            verify(timelineCommandService).appendStatusChanged(any(), any());
        }

        @Test
        @DisplayName("중복 상태 변경 요청이면 예외를 던진다")
        void changeStatus_중복상태_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_STATUS_INVALID);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void changeStatus_서재없음_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void changeStatus_도서없음_예외() {
            User user = UserFixture.user();
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("상태 변경 성공 시 월별 캐시 무효화 이벤트는 발행하지 않는다")
        void changeStatus_성공_이벤트미발행() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.changeReadingStatus(1L, request);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("책 상태별 조회")
    class ViewBooksByStatus {

        @Test
        @DisplayName("첫 조회면 전체 개수를 포함한다")
        void viewBooksByStatus_첫조회_전체개수포함() {
            User user = UserFixture.user();

            Book book1 = BookFixture.book();
            ReflectionTestUtils.setField(book1, "id", 1L);
            ReflectionTestUtils.setField(book1, "title", "테스트 도서1");
            ReflectionTestUtils.setField(book1, "author", "작가1");
            ReflectionTestUtils.setField(book1, "coverImageKey", "book/users/1/cover1.png");

            Book book2 = BookFixture.book();
            ReflectionTestUtils.setField(book2, "id", 2L);
            ReflectionTestUtils.setField(book2, "isbn13", "1234567890124");
            ReflectionTestUtils.setField(book2, "title", "테스트 도서2");
            ReflectionTestUtils.setField(book2, "author", "작가2");
            ReflectionTestUtils.setField(book2, "coverImageKey", "https://example.com/cover2.jpg");

            Library library1 = LibraryFixture.library(user, book1);
            ReflectionTestUtils.setField(library1, "id", 10L);
            ReflectionTestUtils.setField(library1, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library1, "startedAt", LocalDate.of(2025, 1, 1));

            Library library2 = LibraryFixture.library(user, book2);
            ReflectionTestUtils.setField(library2, "id", 9L);
            ReflectionTestUtils.setField(library2, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library2, "startedAt", LocalDate.of(2025, 1, 2));

            int size = 1;
            Slice<Library> slice = new SliceImpl<>(List.of(library1, library2), PageRequest.of(0, size + 1), true);

            given(libraryRepository.findByUserIdAndStatusWithCursor(anyLong(), any(), any(), any())).willReturn(slice);
            given(libraryRepository.countByUserIdAndReadingStatus(anyLong(), any())).willReturn(10L);
            willReturn("https://r2.example.com/cover1.png")
                    .given(presignedUrlService)
                    .resolveImageUrl(1L, "book/users/1/cover1.png");

            LibraryViewDto.StatusBookResponseDto response =
                    libraryQueryService.getBooksByStatus(1L, ReadingStatus.READING, null, size);

            assertThat(response.readingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(response.totalBookNum()).isEqualTo(10);
            assertThat(response.bookItems().items()).hasSize(1);
            assertThat(response.bookItems().items().get(0).coverUrl())
                    .isEqualTo("https://r2.example.com/cover1.png");
            verify(libraryRepository).countByUserIdAndReadingStatus(1L, ReadingStatus.READING);
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/cover1.png");
        }

        @Test
        @DisplayName("커서 조회면 전체 개수를 포함하지 않는다")
        void viewBooksByStatus_커서조회_전체개수미포함() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            ReflectionTestUtils.setField(book, "author", "작가");
            ReflectionTestUtils.setField(book, "coverImageKey", "https://example.com/cover.jpg");

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 8L);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library, "startedAt", LocalDate.of(2025, 1, 1));

            int size = 1;
            Slice<Library> slice = new SliceImpl<>(List.of(library), PageRequest.of(0, size + 1), false);

            given(libraryRepository.findByUserIdAndStatusWithCursor(anyLong(), any(), anyLong(), any())).willReturn(slice);

            LibraryViewDto.StatusBookResponseDto response =
                    libraryQueryService.getBooksByStatus(1L, ReadingStatus.READING, 100L, size);

            assertThat(response.totalBookNum()).isZero();
            verify(libraryRepository, never()).countByUserIdAndReadingStatus(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("책 개수 조회")
    class CountBooks {
        @Test
        @DisplayName("정상")
        void countBooks_정상() {
            User user = UserFixture.user();
            given(libraryRepository.countByUserId(1L)).willReturn(7);

            LibraryViewDto.BookCountResponseDto result = libraryQueryService.getBookCount(1L);

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

            Set<String> result = libraryQueryService.getOwnedIsbns(userId, isbns);

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

            Page<Library> result = libraryQueryService.searchBooksInLibrary(userId, rawKeyword, page, size);

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
            User user = UserFixture.user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book1 = BookFixture.book();
            ReflectionTestUtils.setField(book1, "id", 1L);
            ReflectionTestUtils.setField(book1, "title", "도서1");
            ReflectionTestUtils.setField(book1, "author", "작가1");
            ReflectionTestUtils.setField(book1, "coverImageKey", "cover1");
            Book book2 = BookFixture.book();
            ReflectionTestUtils.setField(book2, "id", 2L);
            ReflectionTestUtils.setField(book2, "isbn13", "1234567890124");
            ReflectionTestUtils.setField(book2, "title", "도서2");
            ReflectionTestUtils.setField(book2, "author", "작가2");
            ReflectionTestUtils.setField(book2, "coverImageKey", "cover2");

            Library library1 = LibraryFixture.library(user, book1);
            Library library2 = LibraryFixture.library(user, book2);

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

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            var result = libraryQueryService.getFocusRecordsByDate(1L, date, null, size);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(30L);
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).focusTime()).isEqualTo("00:02:00");
        }

        @Test
        @DisplayName("마지막 페이지면 hasNext=false이며 null duration은 0으로 반환한다")
        void viewFocusRecordByDate_마지막페이지_및_null_duration_처리() {
            User user = UserFixture.user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            ReflectionTestUtils.setField(book, "title", "도서1");
            ReflectionTestUtils.setField(book, "author", "작가1");
            ReflectionTestUtils.setField(book, "coverImageKey", "cover1");

            Library library = LibraryFixture.library(user, book);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 10L);
            ReflectionTestUtils.setField(focus, "library", library);
            ReflectionTestUtils.setField(focus, "durationSec", null);

            int size = 2;
            Slice<Focus> slice = new SliceImpl<>(List.of(focus), PageRequest.of(0, size + 1), false);
            given(focusRepository.findByLibraryWithCursorByDate(eq(user), eq(date), eq(100L), any(PageRequest.class)))
                    .willReturn(slice);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            var result = libraryQueryService.getFocusRecordsByDate(1L, date, 100L, size);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).focusTime()).isEqualTo("00:00:00");
        }
    }

    @Nested
    @DisplayName("최근 포커스 조회")
    class ViewRecentFocus {

        @Test
        @DisplayName("최근 포커스가 있으면 bookId/title/page를 반환하고 page 0은 null 처리한다")
        void viewRecentFocus_성공_page_null_처리() {
            User user = UserFixture.user();

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 11L);
            ReflectionTestUtils.setField(book, "title", "최근 도서");
            ReflectionTestUtils.setField(book, "author", "작가");
            ReflectionTestUtils.setField(book, "coverImageKey", "cover");

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "page", 0);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 99L);
            ReflectionTestUtils.setField(focus, "library", library);

            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of(focus));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.RecentFocusResponseDto result = libraryQueryService.getRecentFocus(1L);

            assertThat(result).isNotNull();
            assertThat(result.bookId()).isEqualTo(11L);
            assertThat(result.title()).isEqualTo("최근 도서");
            assertThat(result.page()).isNull();
        }

        @Test
        @DisplayName("최근 포커스가 없으면 null을 반환한다")
        void viewRecentFocus_데이터없음_null반환() {
            User user = UserFixture.user();
            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.RecentFocusResponseDto result = libraryQueryService.getRecentFocus(1L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("검색 홈용 최근 포커스 목록을 반환한다")
        void viewRecentFocusBooks_성공() {
            User user = UserFixture.user();

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 11L);
            ReflectionTestUtils.setField(book, "title", "최근 도서");
            ReflectionTestUtils.setField(book, "author", "작가");
            ReflectionTestUtils.setField(book, "coverImageKey", "book/users/1/recent-cover.png");

            Library library = LibraryFixture.library(user, book);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 99L);
            ReflectionTestUtils.setField(focus, "library", library);

            given(focusRepository.findRecentDistinctBooksByUser(eq(user), eq(PageRequest.of(0, 5))))
                    .willReturn(List.of(focus));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/recent-cover.png"))
                    .willReturn("https://cdn.example.com/recent-cover.png");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            List<LibraryViewDto.RecentFocusBookItem> result = libraryQueryService.getRecentFocusBooks(1L, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).bookId()).isEqualTo(11L);
            assertThat(result.get(0).title()).isEqualTo("최근 도서");
            assertThat(result.get(0).author()).isEqualTo("작가");
            assertThat(result.get(0).coverUrl()).isEqualTo("https://cdn.example.com/recent-cover.png");
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/recent-cover.png");
        }
    }

    @Nested
    @DisplayName("읽기 전 도서 5권 조회")
    class ViewBeforeReadingBooks {

        @Test
        @DisplayName("읽기 전 상태 도서 목록을 최대 5건 변환해 반환한다")
        void viewBeforeReadingBooks_성공() {
            User user = UserFixture.user();

            Book firstBook = BookFixture.book();
            ReflectionTestUtils.setField(firstBook, "id", 1L);
            ReflectionTestUtils.setField(firstBook, "isbn13", "1111111111111");
            ReflectionTestUtils.setField(firstBook, "title", "첫 번째");
            ReflectionTestUtils.setField(firstBook, "author", "저자1");
            ReflectionTestUtils.setField(firstBook, "coverImageKey", "book/users/1/first.png");

            Book secondBook = BookFixture.book();
            ReflectionTestUtils.setField(secondBook, "id", 2L);
            ReflectionTestUtils.setField(secondBook, "isbn13", "2222222222222");
            ReflectionTestUtils.setField(secondBook, "title", "두 번째");
            ReflectionTestUtils.setField(secondBook, "author", "저자2");
            ReflectionTestUtils.setField(secondBook, "coverImageKey", "book/users/1/second.png");

            Library firstLibrary = LibraryFixture.library(user, firstBook);
            Library secondLibrary = LibraryFixture.library(user, secondBook);

            given(libraryRepository.findByUserIdAndReadingStatusOrderByIdDesc(
                    1L, ReadingStatus.BEFORE, PageRequest.of(0, 5)))
                    .willReturn(List.of(firstLibrary, secondLibrary));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/first.png"))
                    .willReturn("https://cdn.example.com/first.png");
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/second.png"))
                    .willReturn("https://cdn.example.com/second.png");

            LibraryViewDto.BeforeReadingResponseDto result = libraryQueryService.getBeforeReadingBooks(1L);

            assertThat(result.books()).hasSize(2);
            assertThat(result.books().get(0).coverUrl()).isEqualTo("https://cdn.example.com/first.png");
            assertThat(result.books().get(1).coverUrl()).isEqualTo("https://cdn.example.com/second.png");
        }
    }

    @Nested
    @DisplayName("독서 연도 조회")
    class ViewReadingYears {

        @Test
        @DisplayName("가입 연도부터 현재 연도까지 오름차순으로 반환한다")
        void viewReadingYears_성공() {
            User user = UserFixture.user();
            ReflectionTestUtils.setField(user, "createdDate", LocalDateTime.of(2024, 1, 1, 0, 0));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.YearResponseDto result = libraryQueryService.getReadingYears(user.getId());

            int currentYear = LocalDateTime.now().getYear();
            assertThat(result.years().get(0)).isEqualTo(2024);
            assertThat(result.years().get(result.years().size() - 1)).isEqualTo(currentYear);
            assertThat(result.years()).contains(2025);
        }
    }
}
