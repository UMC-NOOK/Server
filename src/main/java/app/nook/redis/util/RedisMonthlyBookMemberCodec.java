package app.nook.redis.util;

import app.nook.redis.dto.RedisDayRow;
import app.nook.redis.exception.RedisErrorCode;
import app.nook.redis.exception.RedisOperationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

public final class RedisMonthlyBookMemberCodec {

    private RedisMonthlyBookMemberCodec() {
    }

    public static String encode(LocalDate date, long bookCount, Long topBookId, String topCoverUrl) {
        String topCoverUrlEncoded = Base64.getUrlEncoder()
                .encodeToString(safe(topCoverUrl).getBytes(StandardCharsets.UTF_8));

        return date + ":" + bookCount + ":" + (topBookId == null ? "null" : topBookId) + ":" + topCoverUrlEncoded;
    }

    public static RedisDayRow decode(String value) {
        if (value == null || value.isBlank()) {
            throw new RedisOperationException(
                    RedisErrorCode.REDIS_SERIALIZATION_FAILED,
                    new IllegalArgumentException()
            );
        }
        String[] tokens = value.split(":", 4);
        if (tokens.length < 4) {
            throw new RedisOperationException(
                    RedisErrorCode.REDIS_SERIALIZATION_FAILED,
                    new IllegalArgumentException()
            );
        }
        try {
            LocalDate date = LocalDate.parse(tokens[0]);
            long bookCount = Long.parseLong(tokens[1]);
            Long topBookId = "null".equals(tokens[2]) ? null : Long.parseLong(tokens[2]);
            String coverUrl = new String(Base64.getUrlDecoder().decode(tokens[3]), StandardCharsets.UTF_8);
            if (coverUrl.isBlank()) {
                coverUrl = null;
            }
            return new RedisDayRow(date, bookCount, topBookId, coverUrl);
        } catch (RuntimeException e) {
            throw new RedisOperationException(RedisErrorCode.REDIS_SERIALIZATION_FAILED, e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

}
