package app.nook.global.fixture;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.library.domain.Library;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class FocusFixture {

    public static Book book() {
        Book book = Book.builder()
                .title("첫사랑의 침공")
                .author("권혁일")
                .build();
        ReflectionTestUtils.setField(book, "id", 20L);
        return book;
    }

    public static Focus focus(Library library) {
        Focus focus = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 3, 22, 14, 0, 0))
                .durationSec(0)
                .build();
        ReflectionTestUtils.setField(focus, "id", 100L);
        return focus;
    }

    public static Focus completedFocus(Library library) {
        Focus focus = Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 3, 22, 14, 0, 0))
                .endedAt(LocalDateTime.of(2026, 3, 22, 14, 34, 26))
                .durationSec(2066)
                .build();
        ReflectionTestUtils.setField(focus, "id", 100L);
        return focus;
    }
}
