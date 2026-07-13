package app.nook.book.facade;

import app.nook.book.domain.Book;
import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.service.BookService;
import app.nook.library.service.LibraryCommandService;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserBookFacadeTest {

    @Mock private BookService bookService;
    @Mock private LibraryCommandService libraryCommandService;
    @Mock private PresignedUrlService presignedUrlService;

    @InjectMocks private UserBookFacade userBookFacade;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .nickName("tester")
                .provider("google")
                .providerId("pid")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void createUserBook_success() {
        BookRequestDto.CreateUserBookRequest req = new BookRequestDto.CreateUserBookRequest(
                "혼모노",
                "성해은",
                "소설/시/희곡",
                "소개",
                348,
                "민음사",
                "2023-06-05",
                "9788936439743",
                null
        );

        Book saved = Book.builder().title("혼모노").build();
        ReflectionTestUtils.setField(saved, "id", 100L);

        BookResponseDto.BookDetailDto detail = BookResponseDto.BookDetailDto.builder()
                .bookId(100L).title("혼모노").build();

        given(bookService.createUserBook(eq(user), any(), isNull())).willReturn(saved);
        given(bookService.getBookDetailByIdForUserBookResponse(user, 100L)).willReturn(detail);

        BookResponseDto.BookDetailDto result = userBookFacade.createUserBook(user, req);

        verify(presignedUrlService, never()).validateOwnedImageKey(anyLong(), any(), any());
        verify(bookService).createUserBook(eq(user), any(), isNull());
        verify(libraryCommandService).registerBook(1L, 100L);
        verify(bookService).getBookDetailByIdForUserBookResponse(user, 100L);
        verify(bookService, never()).getBookDetailById(any(), anyLong());
        assertThat(result.getBookId()).isEqualTo(100L);
    }

    @Test
    void updateUserBook_success() {
        BookRequestDto.UpdateUserBookRequest req = new BookRequestDto.UpdateUserBookRequest(
                "혼모노 수정",
                "성해은",
                "소설/시/희곡",
                "소개수정",
                360,
                "민음사",
                "2024-01-01",
                "9788936439743",
                null
        );

        Book updated = Book.builder().title("혼모노 수정").build();
        ReflectionTestUtils.setField(updated, "id", 100L);

        BookResponseDto.BookDetailDto detail = BookResponseDto.BookDetailDto.builder()
                .bookId(100L).title("혼모노 수정").build();

        willDoNothing().given(bookService)
                .updateUserBook(eq(user), eq(100L), any(BookRequestDto.UpdateUserBookRequest.class), isNull());
        given(bookService.getBookDetailByIdForUserBookResponse(user, 100L)).willReturn(detail);

        BookResponseDto.BookDetailDto result = userBookFacade.updateUserBook(user, 100L, req);

        verify(presignedUrlService, never()).validateOwnedImageKey(anyLong(), any(), any());
        verify(bookService).updateUserBook(eq(user), eq(100L), any(BookRequestDto.UpdateUserBookRequest.class), isNull());
        verify(bookService).getBookDetailByIdForUserBookResponse(user, 100L);
        verify(bookService, never()).getBookDetailById(any(), anyLong());
        assertThat(result.getBookId()).isEqualTo(100L);
    }

}
