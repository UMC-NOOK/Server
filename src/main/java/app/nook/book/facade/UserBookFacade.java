package app.nook.book.facade;

import app.nook.book.domain.Book;
import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.service.BookService;
import app.nook.library.service.LibraryService;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserBookFacade {

    private final BookService bookService;
    private final LibraryService libraryService;
    private final PresignedUrlService presignedUrlService;

    // 표지 key 저장 + USER 도서 저장 + 서재 자동등록
    @Transactional
    public BookResponseDto.BookDetailDto createUserBook(User user, BookRequestDto.CreateUserBookRequest request) {
        boolean hasCover = request.coverImageKey() != null && !request.coverImageKey().isBlank();
        log.info("[USER_BOOK_CREATE_START] userId={}, hasCover={}", user.getId(), hasCover);

        String coverImageKey = hasCover ? request.coverImageKey().trim() : null;
        if (coverImageKey != null) {
            presignedUrlService.validateOwnedImageKey(user.getId(), coverImageKey, "book");
        }

        Book book = bookService.createUserBook(user, request, coverImageKey);
        log.info("[USER_BOOK_CREATE_SAVED] userId={}, bookId={}", user.getId(), book.getId());

        // 신규 생성 도서는 생성 즉시 내 서재로 등록
        libraryService.save(user, book.getId());

        return bookService.getBookDetailById(user, book.getId());
    }

    // USER 도서 수정(업로드/도서 수정/상세 조회)
    @Transactional
    public BookResponseDto.BookDetailDto updateUserBook(
            User user, Long bookId, BookRequestDto.UpdateUserBookRequest request) {
        boolean hasNewCover = request.coverImageKey() != null && !request.coverImageKey().isBlank();
        log.info("[USER_BOOK_UPDATE_START] userId={}, bookId={}, hasNewCover={}", user.getId(), bookId, hasNewCover);

        String newCoverKey = hasNewCover ? request.coverImageKey().trim() : null;
        if (newCoverKey != null) {
            presignedUrlService.validateOwnedImageKey(user.getId(), newCoverKey, "book");
        }
        bookService.updateUserBook(user, bookId, request, newCoverKey);

        log.info("[USER_BOOK_UPDATE_DONE] userId={}, bookId={}", user.getId(), bookId);
        return bookService.getBookDetailById(user, bookId);
    }
}
