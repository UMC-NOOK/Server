package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class BookAccessService {

    public void assertCanView(User user, Book book) {
        if (isPublicBook(book) || isCreatedBy(user, book)) {
            return;
        }

        throw new CustomException(BookErrorCode.BOOK_ACCESS_DENIED);
    }

    public void assertCanAddToLibrary(User user, Book book) {
        if (isPublicBook(book) || isCreatedBy(user, book)) {
            return;
        }

        throw new CustomException(BookErrorCode.BOOK_ACCESS_DENIED);
    }

    public void assertCanUpdate(User user, Book book) {
        if (book != null && book.getSourceType() == SourceType.USER && isCreatedBy(user, book)) {
            return;
        }

        throw new CustomException(BookErrorCode.BOOK_NOT_OWNED);
    }

    private boolean isPublicBook(Book book) {
        return book != null && book.getSourceType() == SourceType.ALADIN;
    }

    private boolean isCreatedBy(User user, Book book) {
        return user != null
                && user.getId() != null
                && book != null
                && book.getCreatedByUserId() != null
                && book.getCreatedByUserId().equals(user.getId());
    }
}
