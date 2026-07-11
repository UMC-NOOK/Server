package app.nook.library.dto;

import java.time.LocalDateTime;

public record LibraryBookCursor(
        Long libraryId,
        LocalDateTime lastFocusedAt,
        Long recordCount,
        String title
) {
    public boolean isEmpty() {
        return libraryId == null;
    }
}
