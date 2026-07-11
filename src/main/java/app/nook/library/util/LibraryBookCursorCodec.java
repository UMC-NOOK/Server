package app.nook.library.util;

import app.nook.global.exception.CustomException;
import app.nook.global.response.CommonErrorCode;
import app.nook.library.dto.LibraryBookCursor;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public final class LibraryBookCursorCodec {

    private LibraryBookCursorCodec() {}

    public static String encode(LibraryBookCursor cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        String payload = valueOf(cursor.libraryId())
                + "|" + valueOf(cursor.lastFocusedAt())
                + "|" + valueOf(cursor.recordCount())
                + "|" + encodedTitle(cursor.title());

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static LibraryBookCursor decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 4) {
                throw new CustomException(CommonErrorCode.INVALID_REQUEST);
            }

            Long libraryId = parseLong(parts[0]);
            LocalDateTime lastFocusedAt = parseDateTime(parts[1]);
            Long recordCount = parseLong(parts[2]);
            String title = parseTitle(parts[3]);

            LibraryBookCursor cursor = new LibraryBookCursor(libraryId, lastFocusedAt, recordCount, title);
            return cursor.isEmpty() ? null : cursor;
        } catch (IllegalArgumentException e) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String encodedTitle(String title) {
        if (title == null || title.isBlank()) return "";
        return URLEncoder.encode(title, StandardCharsets.UTF_8);
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        return Long.parseLong(value);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value);
    }

    private static String parseTitle(String value) {
        if (value == null || value.isBlank()) return null;
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
