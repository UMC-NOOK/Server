package app.nook.global.fixture;

import app.nook.book.domain.Book;
import org.springframework.test.util.ReflectionTestUtils;

public class BookFixture {

    public static Book book() {
        Book book = Book.builder()
                .title("테스트 책")
                .build();
        ReflectionTestUtils.setField(book, "id", 10L);
        return book;
    }
}
