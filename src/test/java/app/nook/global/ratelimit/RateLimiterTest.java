package app.nook.global.ratelimit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RateLimiterTest {

    @Test
    @SuppressWarnings("unchecked")
    void Redis_장애시_요청을_허용한다() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .willThrow(new RedisConnectionFailureException("down"));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiter rateLimiter = new RateLimiter(redisTemplate, meterRegistry, Clock.systemUTC());

        assertThatCode(() -> rateLimiter.consume(List.of(RateLimitPolicy.IMAGE_MINUTE, RateLimitPolicy.IMAGE_DAY), "1", 3))
                .doesNotThrowAnyException();
        assertThat(meterRegistry.counter("rate_limit.failopen").count()).isEqualTo(1.0);
    }
}
