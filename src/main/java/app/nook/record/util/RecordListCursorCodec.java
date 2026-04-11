package app.nook.record.util;

import app.nook.global.exception.CustomException;
import app.nook.global.response.CommonErrorCode;
import app.nook.record.dto.RecordListCursor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public final class RecordListCursorCodec {

    private RecordListCursorCodec() {}

    public static String encode(RecordListCursor cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        String payload = valueOf(cursor.lastCount())
                + "|" + valueOf(cursor.bookId())
                + "|" + valueOf(cursor.lastCreatedDate());

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static RecordListCursor decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3) {
                throw new CustomException(CommonErrorCode.INVALID_REQUEST);
            }

            Long lastCount = parseLong(parts[0]);
            Long bookId = parseLong(parts[1]);
            LocalDateTime lastCreatedDate = parseDateTime(parts[2]);

            RecordListCursor cursor = new RecordListCursor(lastCount, bookId, lastCreatedDate);
            return cursor.isEmpty() ? null : cursor;
        } catch (IllegalArgumentException exception) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }
}
