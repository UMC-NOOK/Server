package app.nook.library.util;

public class FocusTimeUtil {

    private static final int SECONDS_IN_HOUR = 3600;

    public static String formatFocusTime(int focusSec) {
        return formatFocusTime((long) focusSec);
    }

    public static String formatFocusTime(long focusSec) {
        long hours = focusSec / SECONDS_IN_HOUR;
        long minutes = (focusSec % SECONDS_IN_HOUR) / 60;
        long seconds = focusSec % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
