package app.nook.timeline.util;

public final class TimelinePreviewText {

    public static final int RECORD_MAX_LENGTH = 100;

    private TimelinePreviewText() {
    }

    public static String truncate(String text) {
        if (text == null || text.codePointCount(0, text.length()) <= RECORD_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, RECORD_MAX_LENGTH));
    }
}
