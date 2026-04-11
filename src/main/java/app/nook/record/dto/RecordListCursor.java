package app.nook.record.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// 기록 복합 키 커서
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
