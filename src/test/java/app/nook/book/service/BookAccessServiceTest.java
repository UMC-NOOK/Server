package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookAccessServiceTest {

    private final BookAccessService bookAccessService = new BookAccessService();

    @Test
    @DisplayName("ALADIN 도서는 조회 권한을 허용한다")
    void assertCanView_aladinBook_success() {
        User user = createUser(1L);
        Book book = createBook(SourceType.ALADIN, null);

        assertThatCode(() -> bookAccessService.assertCanView(user, book))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("본인이 생성한 USER 도서는 조회 권한을 허용한다")
    void assertCanView_ownedUserBook_success() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 1L);

        assertThatCode(() -> bookAccessService.assertCanView(user, book))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 사용자가 생성한 USER 도서는 조회 권한을 차단한다")
    void assertCanView_otherUserBook_fail() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 2L);

        assertThatThrownBy(() -> bookAccessService.assertCanView(user, book))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(BookErrorCode.BOOK_ACCESS_DENIED);
    }

    @Test
    @DisplayName("로그인 사용자가 없으면 USER 도서 조회 권한을 차단한다")
    void assertCanView_anonymousUserBook_fail() {
        Book book = createBook(SourceType.USER, 1L);

        assertThatThrownBy(() -> bookAccessService.assertCanView(null, book))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(BookErrorCode.BOOK_ACCESS_DENIED);
    }

    @Test
    @DisplayName("본인이 생성한 USER 도서는 서재 등록 권한을 허용한다")
    void assertCanAddToLibrary_ownedUserBook_success() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 1L);

        assertThatCode(() -> bookAccessService.assertCanAddToLibrary(user, book))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 사용자가 생성한 USER 도서는 서재 등록 권한을 차단한다")
    void assertCanAddToLibrary_otherUserBook_fail() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 2L);

        assertThatThrownBy(() -> bookAccessService.assertCanAddToLibrary(user, book))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(BookErrorCode.BOOK_ACCESS_DENIED);
    }

    @Test
    @DisplayName("본인이 생성한 USER 도서는 수정 권한을 허용한다")
    void assertCanUpdate_ownedUserBook_success() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 1L);

        assertThatCode(() -> bookAccessService.assertCanUpdate(user, book))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ALADIN 도서는 수정 권한을 차단한다")
    void assertCanUpdate_aladinBook_fail() {
        User user = createUser(1L);
        Book book = createBook(SourceType.ALADIN, null);

        assertThatThrownBy(() -> bookAccessService.assertCanUpdate(user, book))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(BookErrorCode.BOOK_NOT_OWNED);
    }

    @Test
    @DisplayName("다른 사용자가 생성한 USER 도서는 수정 권한을 차단한다")
    void assertCanUpdate_otherUserBook_fail() {
        User user = createUser(1L);
        Book book = createBook(SourceType.USER, 2L);

        assertThatThrownBy(() -> bookAccessService.assertCanUpdate(user, book))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(BookErrorCode.BOOK_NOT_OWNED);
    }

    private User createUser(Long id) {
        User user = User.builder()
                .email("test@example.com")
                .nickName("tester")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Book createBook(SourceType sourceType, Long createdByUserId) {
        return Book.builder()
                .title("테스트 도서")
                .author("테스트 저자")
                .sourceType(sourceType)
                .createdByUserId(createdByUserId)
                .build();
    }
}
