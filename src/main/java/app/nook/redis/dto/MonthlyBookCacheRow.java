package app.nook.redis.dto;

import java.time.LocalDate;

public record MonthlyBookCacheRow(
        LocalDate date,
        long bookCount,
        Long topBookId,
        String topCoverUrl,
        long topFocusSec
) {
}
