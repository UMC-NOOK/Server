package app.nook.library.util;

public class FocusTimeUtil {

    private static final int SECONDS_IN_HOUR = 3600;

    public static String formatFocusTime(int focusSec) {
        int hours = focusSec / SECONDS_IN_HOUR;
        int minutes = (focusSec % SECONDS_IN_HOUR) / 60;
        int seconds = focusSec % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
