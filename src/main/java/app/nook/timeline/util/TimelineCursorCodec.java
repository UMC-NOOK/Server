package app.nook.timeline.util;

import app.nook.global.exception.CustomException;
import app.nook.global.response.CommonErrorCode;
import app.nook.timeline.dto.TimelineCursor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class TimelineCursorCodec {

    private TimelineCursorCodec() {}

    public static String encode(TimelineCursor cursor) {
        String payload = cursor.occurredAt() + "|" + cursor.timelineId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static TimelineCursor decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            LocalDateTime occurredAt = LocalDateTime.parse(parts[0]);
            long timelineId = Long.parseLong(parts[1]);
            if (timelineId <= 0) {
                throw new IllegalArgumentException();
            }
            return new TimelineCursor(occurredAt, timelineId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
