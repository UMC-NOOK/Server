package app.nook.record.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record RecordListCursor(
        Long lastCount,
        Long bookId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime lastCreatedDate
) {
    public boolean isEmpty() {
        return lastCount == null && bookId == null && lastCreatedDate == null;
    }
}
