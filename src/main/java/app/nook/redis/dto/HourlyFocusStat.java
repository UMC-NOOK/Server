package app.nook.redis.dto;

public record HourlyFocusStat(
        int hour,
        long totalSec
) {
}
