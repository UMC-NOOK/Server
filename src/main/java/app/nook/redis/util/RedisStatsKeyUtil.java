package app.nook.redis.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class RedisStatsKeyUtil {
    private static final DateTimeFormatter YYYYMM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private RedisStatsKeyUtil() {
    }

    public static String monthlyBooksZsetKey(Long userId, YearMonth yearMonth) {
        return "stats:library:monthly:zset:" + userId + ":" + yearMonth;
    }

    public static String monthlyBooksTotalKey(Long userId, YearMonth yearMonth) {
        return "stats:library:monthly:total:" + userId + ":" + yearMonth;
    }

    public static String monthlyBooksExistsKey(Long userId, YearMonth yearMonth) {
        return "stats:library:monthly:exists:" + userId + ":" + yearMonth;
    }

    public static String monthlyFocusDailyKey(Long userId, YearMonth yearMonth) {
        return "stats:focus:daily:" + userId + ":" + yearMonth.format(YYYYMM_FORMATTER);
    }

    public static String monthlyFocusDailyExistsKey(Long userId, YearMonth yearMonth) {
        return "stats:focus:daily:exists:" + userId + ":" + yearMonth.format(YYYYMM_FORMATTER);
    }

    public static String monthlyFocusHourlyKey(Long userId, YearMonth yearMonth) {
        return "stats:focus:hourly:" + userId + ":" + yearMonth.format(YYYYMM_FORMATTER);
    }
}
