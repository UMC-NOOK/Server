package app.nook.redis.dto;

public record ScoredRedisDayRow(
        double score,
        RedisDayRow row
) {
}
