package app.nook.global.fixture;

import app.nook.book.domain.Book;
import app.nook.library.domain.Library;
import app.nook.user.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

public class LibraryFixture {

    public static Library library() {
        User user = UserFixture.user();   // 픽스처끼리 참조
        Book book = BookFixture.book();
        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 20L);
        return library;
    }

    // 특정 user/book으로 생성
    public static Library library(User user, Book book) {
        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 20L);
        return library;
    }
}