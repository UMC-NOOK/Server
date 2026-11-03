package app.nook.global.ratelimit;

import app.nook.global.exception.RateLimitExceededException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static app.nook.global.ratelimit.RateLimitPolicy.IMAGE_DAY;
import static app.nook.global.ratelimit.RateLimitPolicy.IMAGE_MINUTE;
import static app.nook.global.ratelimit.RateLimitPolicy.LOGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lua 제한기 통합 테스트 (실제 Redis, Docker 필요)
 */
@Testcontainers
class RateLimiterRedisTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    // 2026-11-02 23:59:30 KST: 일일 키는 30초 뒤 자정에 만료
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T14:59:30Z"), ZoneId.of("Asia/Seoul"));
    private static final String DAY_KEY = "rl:image-day:1:20261102";
    private static final String MINUTE_KEY = "rl:image-minute:1";

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, String> redisTemplate;
    private RateLimiter rateLimiter;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushDb();
            return null;
        }, true);
        rateLimiter = new RateLimiter(redisTemplate, new SimpleMeterRegistry(), CLOCK);
    }

    @Test
    void 한도_안에서는_허용하고_초과하면_거부하며_거부된_요청은_세지_않는다() {
        for (int i = 0; i < LOGIN.getLimit(); i++) {
            rateLimiter.consume(LOGIN, "1.2.3.4");
        }

        assertThatThrownBy(() -> rateLimiter.consume(LOGIN, "1.2.3.4"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(e -> {
                    RateLimitExceededException ex = (RateLimitExceededException) e;
                    assertThat(ex.getTitle()).isEqualTo("요청 한도에 도달했습니다.");
                    assertThat(ex.getDescription()).matches("\\d+초 후 다시 시도해 주세요\\.|1분 후 다시 시도해 주세요\\.");
                    assertThat(ex.getRetryAfterSeconds()).isBetween(1L, 60L);
                });
        assertThatThrownBy(() -> rateLimiter.consume(LOGIN, "1.2.3.4"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThat(redisTemplate.opsForValue().get("rl:login:1.2.3.4")).isEqualTo("30");
        assertThat(redisTemplate.getExpire("rl:login:1.2.3.4")).isBetween(1L, 60L);
        // 다른 subject는 별도 집계
        rateLimiter.consume(LOGIN, "5.6.7.8");
    }

    @Test
    void 이미지_분당_한도_초과시_두_키_모두_증가하지_않는다() {
        rateLimiter.consume(List.of(IMAGE_MINUTE, IMAGE_DAY), "1", 18);

        assertThatThrownBy(() -> rateLimiter.consume(List.of(IMAGE_MINUTE, IMAGE_DAY), "1", 3))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("요청 한도에 도달했습니다. 1분 후 다시 시도해 주세요.");

        assertThat(redisTemplate.opsForValue().get(MINUTE_KEY)).isEqualTo("18");
        assertThat(redisTemplate.opsForValue().get(DAY_KEY)).isEqualTo("18");
    }

    @Test
    void 이미지_일일_한도_초과시_두_키_모두_증가하지_않고_일일_문구를_반환한다() {
        redisTemplate.opsForValue().set(DAY_KEY, "98");

        assertThatThrownBy(() -> rateLimiter.consume(List.of(IMAGE_MINUTE, IMAGE_DAY), "1", 5))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("이미지 업로드 한도에 도달했습니다. 내일 다시 시도해 주세요.");

        assertThat(redisTemplate.hasKey(MINUTE_KEY)).isFalse();
        assertThat(redisTemplate.opsForValue().get(DAY_KEY)).isEqualTo("98");
    }

    @Test
    void 이미지_일일_키는_KST_날짜로_만들고_자정까지_유지한다() {
        rateLimiter.consume(List.of(IMAGE_MINUTE, IMAGE_DAY), "1", 2);

        assertThat(redisTemplate.opsForValue().get(DAY_KEY)).isEqualTo("2");
        assertThat(redisTemplate.getExpire(DAY_KEY)).isBetween(1L, 30L);
        assertThat(redisTemplate.getExpire(MINUTE_KEY)).isBetween(31L, 60L);
    }

    @Test
    void 요청_장수가_한도보다_크면_키가_없어도_거부하고_윈도우만큼_안내한다() {
        assertThatThrownBy(() -> rateLimiter.consume(List.of(IMAGE_MINUTE, IMAGE_DAY), "1", 21))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("요청 한도에 도달했습니다. 1분 후 다시 시도해 주세요.");

        assertThat(redisTemplate.hasKey(MINUTE_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(DAY_KEY)).isFalse();
    }
}
