package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.book.repository.BookRepository;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTimeLineRepository bookTimeLineRepository;

    @InjectMocks
    private LibraryService libraryService;

    @Test
    void save_성공() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

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

        verify(bookTimeLineRepository).save(any());
    }

    @Test
    void save_도서없음_예외() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> libraryService.save(user, 1L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void save_이미등록_예외() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

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

        CustomException ex = assertThrows(
                CustomException.class,
                () -> libraryService.save(user, 1L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_ALREADY_EXIST);
    }

    @Test
    void deleteById_성공() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

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

        libraryService.deleteById(user, 1L);

        verify(libraryRepository).delete(library);
    }

    @Test
    void deleteById_서재없음_예외() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

        Book book = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서")
                .build();

        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);

        CustomException ex = assertThrows(
                CustomException.class,
                () -> libraryService.deleteById(user, 1L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
    }

    @Test
    void changeStatus_성공() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

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
        verify(bookTimeLineRepository).save(any());
    }

    @Test
    void changeStatus_중복상태_예외() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

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

        CustomException ex = assertThrows(
                CustomException.class,
                () -> libraryService.changeStatus(user, request)
        );

        assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_STATUS_INVALID);
    }

    @Test
    void changeStatus_서재없음_예외() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

        Book book = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서")
                .build();

        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);

        CustomException ex = assertThrows(
                CustomException.class,
                () -> libraryService.changeStatus(user, request)
        );

        assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
    }

    @Test
    void viewBooksByStatus_첫조회_전체개수포함() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

        Book book1 = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서1")
                .author("작가1")
                .coverImageUrl("https://example.com/cover1.jpg")
                .build();
        ReflectionTestUtils.setField(book1, "id", 1L);

        Book book2 = Book.builder()
                .isbn13("1234567890124")
                .title("테스트 도서2")
                .author("작가2")
                .coverImageUrl("https://example.com/cover2.jpg")
                .build();
        ReflectionTestUtils.setField(book2, "id", 2L);

        Library library1 = Library.builder()
                .user(user)
                .book(book1)
                .build();
        ReflectionTestUtils.setField(library1, "id", 10L);
        ReflectionTestUtils.setField(library1, "readingStatus", ReadingStatus.READING);
        ReflectionTestUtils.setField(library1, "startedAt", LocalDate.of(2025, 1, 1));

        Library library2 = Library.builder()
                .user(user)
                .book(book2)
                .build();
        ReflectionTestUtils.setField(library2, "id", 9L);
        ReflectionTestUtils.setField(library2, "readingStatus", ReadingStatus.READING);
        ReflectionTestUtils.setField(library2, "startedAt", LocalDate.of(2025, 1, 2));

        int size = 1;
        Slice<Library> slice = new SliceImpl<>(
                List.of(library1, library2),
                PageRequest.of(0, size + 1),
                true
        );

        given(libraryRepository.findByStatusWithCursor(any(), any(), any(), any()))
                .willReturn(slice);
        given(libraryRepository.countByUserAndReadingStatus(any(), any()))
                .willReturn(10L);

        LibraryViewDto.StatusBookResponseDto response =
                libraryService.viewBooksByStatus(user, ReadingStatus.READING, null, size);

        assertThat(response.readingStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(response.totalBookNum()).isEqualTo(10);
        assertThat(response.bookItems().getItems()).hasSize(1);

        verify(libraryRepository).countByUserAndReadingStatus(user, ReadingStatus.READING);
    }

    @Test
    void viewBooksByStatus_커서조회_전체개수미포함() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();

        Book book = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서")
                .author("작가")
                .coverImageUrl("https://example.com/cover.jpg")
                .build();
        ReflectionTestUtils.setField(book, "id", 1L);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 8L);
        ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);
        ReflectionTestUtils.setField(library, "startedAt", LocalDate.of(2025, 1, 1));

        int size = 1;
        Slice<Library> slice = new SliceImpl<>(
                List.of(library),
                PageRequest.of(0, size + 1),
                false
        );

        given(libraryRepository.findByStatusWithCursor(any(), any(), anyLong(), any()))
                .willReturn(slice);

        LibraryViewDto.StatusBookResponseDto response =
                libraryService.viewBooksByStatus(user, ReadingStatus.READING, 100L, size);

        assertThat(response.totalBookNum()).isZero();
        verify(libraryRepository, never()).countByUserAndReadingStatus(any(), any());
    }
}
