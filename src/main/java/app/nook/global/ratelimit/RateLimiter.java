package app.nook.global.ratelimit;

import app.nook.global.exception.RateLimitExceededException;
import app.nook.global.response.CommonErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis Lua 고정 윈도우 요청 제한기
 * - 여러 정책을 한 번에 검사해 모두 한도 안일 때만 cost만큼 증가 (거부된 요청은 집계하지 않음)
 * - Redis 장애 시 fail-open (요청 허용 + warn 로그 + 메트릭)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String IMAGE_DAY_TITLE = "이미지 업로드 한도에 도달했습니다.";
    private static final String IMAGE_DAY_DESCRIPTION = "내일 다시 시도해 주세요.";

    // KEYS: 카운터 키, ARGV[1]: cost, ARGV[1+i]: 한도, ARGV[1+n+i]: TTL(ms)
    // 반환: {0, 0} 허용 / {초과한 키 번호, 남은 ms} 거부 (여러 키 초과 시 남은 시간이 가장 긴 키)
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local cost = tonumber(ARGV[1])
            local n = #KEYS
            local rejected, longest = 0, 0
            for i = 1, n do
              local current = tonumber(redis.call('GET', KEYS[i]) or '0')
              if current + cost > tonumber(ARGV[1 + i]) then
                local ttl = redis.call('PTTL', KEYS[i])
                if ttl < 0 then ttl = tonumber(ARGV[1 + n + i]) end
                if ttl > longest then rejected, longest = i, ttl end
              end
            end
            if rejected > 0 then
              return {rejected, longest}
            end
            for i = 1, n do
              redis.call('INCRBY', KEYS[i], cost)
              if redis.call('PTTL', KEYS[i]) < 0 then
                redis.call('PEXPIRE', KEYS[i], ARGV[1 + n + i])
              end
            end
            return {0, 0}
            """, List.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public void consume(RateLimitPolicy policy, String subject) {
        consume(List.of(policy), subject, 1);
    }

    /** 여러 정책을 원자적으로 함께 검사한다. 하나라도 넘으면 어느 키도 증가하지 않는다. */
    public void consume(List<RateLimitPolicy> policies, String subject, int cost) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(KST));
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(cost));
        for (RateLimitPolicy policy : policies) {
            keys.add(key(policy, subject, now));
            args.add(String.valueOf(policy.getLimit()));
        }
        for (RateLimitPolicy policy : policies) {
            args.add(String.valueOf(ttl(policy, now).toMillis()));
        }

        List<?> result;
        try {
            result = redisTemplate.execute(SCRIPT, keys, args.toArray());
        } catch (RuntimeException e) {
            log.warn("Rate limit check failed, allowing request: policies={}", policies, e);
            meterRegistry.counter("rate_limit.failopen").increment();
            return;
        }

        int rejectedIndex = ((Number) result.get(0)).intValue();
        if (rejectedIndex == 0) {
            return;
        }
        RateLimitPolicy rejected = policies.get(rejectedIndex - 1);
        long retryAfterSeconds = (((Number) result.get(1)).longValue() + 999) / 1000;
        meterRegistry.counter("rate_limit.rejected", "policy", rejected.name()).increment();

        if (rejected == RateLimitPolicy.IMAGE_DAY) {
            throw new RateLimitExceededException(
                    CommonErrorCode.TOO_MANY_REQUESTS, IMAGE_DAY_TITLE, IMAGE_DAY_DESCRIPTION, retryAfterSeconds);
        }
        throw RateLimitExceededException.retryAfter(
                CommonErrorCode.TOO_MANY_REQUESTS, CommonErrorCode.TOO_MANY_REQUESTS.getMessage(), retryAfterSeconds);
    }

    private static String key(RateLimitPolicy policy, String subject, ZonedDateTime now) {
        String key = "rl:" + policy.keyName() + ":" + subject;
        return policy == RateLimitPolicy.IMAGE_DAY ? key + ":" + now.format(DAY_FORMAT) : key;
    }

    private static Duration ttl(RateLimitPolicy policy, ZonedDateTime now) {
        if (policy.getWindow() != null) {
            return policy.getWindow();
        }
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(KST));
    }
}
