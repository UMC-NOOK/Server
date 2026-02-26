package app.nook.book.utils;

public class BookUtils {
    private BookUtils() {}

    public static String normalizeIsbn(String rawIsbn) {
        if (rawIsbn == null) {
            return null;
        }
        String trimmed = rawIsbn.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
