package app.nook.redis.dto;

import java.time.LocalDate;

public record RedisDayRow(
        LocalDate date,
        Long bookCount,
        Long topBookId,
        String topCoverUrl
) {
}
