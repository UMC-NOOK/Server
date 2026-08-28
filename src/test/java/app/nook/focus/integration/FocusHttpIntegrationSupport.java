package app.nook.focus.integration;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.ThemeRepository;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.redis.util.RedisStatsKeyUtil;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class FocusHttpIntegrationSupport extends AbstractPostgresContainerTests {

    protected static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 8, 1, 23, 55);
    protected static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 8, 2, 0, 10);
    protected static final YearMonth AFFECTED_MONTH = YearMonth.of(2026, 8);
    protected static final YearMonth UNRELATED_MONTH = YearMonth.of(2026, 7);

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired protected TestRestTemplate restTemplate;
    @Autowired protected JwtProvider jwtProvider;
    @Autowired protected MutableKstClock clock;
    @Autowired protected UserRepository userRepository;
    @Autowired protected BookRepository bookRepository;
    @Autowired protected LibraryRepository libraryRepository;
    @Autowired protected ThemeRepository themeRepository;
    @Autowired protected FocusRepository focusRepository;
    @Autowired protected TimelineRepository timelineRepository;
    @Autowired protected StringRedisTemplate redis;
    @Autowired protected RedisConnectionFactory redisConnectionFactory;
    @Autowired protected PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void clearPersistentState() {
        timelineRepository.deleteAll();
        focusRepository.deleteAll();
        libraryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        themeRepository.deleteAll();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
        clock.set(STARTED_AT);
    }

    protected Fixture fixture() {
        User user = userRepository.save(User.builder()
                .provider("integration")
                .providerId("focus-kst")
                .email("focus-kst@nook.test")
                .nickName("focus-reader")
                .role(UserRole.USER)
                .build());
        Book book = bookRepository.save(Book.builder()
                .isbn13("9780000000001")
                .title("Midnight Book")
                .author("NOOK")
                .pages(300)
                .sourceType(SourceType.USER)
                .createdByUserId(user.getId())
                .build());
        Library library = libraryRepository.save(new Library(user, book));
        Theme theme = themeRepository.save(Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("theme-1.png")
                .build());
        return new Fixture(user, library, theme, jwtProvider.createAccessToken(user));
    }

    protected ResponseEntity<JsonNode> start(Fixture fixture) {
        Map<String, Object> body = Map.of(
                "libraryId", fixture.library().getId(),
                "themeId", fixture.theme().getId()
        );
        return post("/api/v1/focuses/start", body, fixture.accessToken());
    }

    protected ResponseEntity<JsonNode> end(long focusId, String accessToken) {
        Map<String, Object> body = Map.of(
                "focusId", focusId,
                "page", 72,
                "isFinished", true
        );
        return post("/api/v1/focuses/end", body, accessToken);
    }

    protected ResponseEntity<JsonNode> post(String path, Object body, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), JsonNode.class);
    }

    protected long focusId(ResponseEntity<JsonNode> startResponse) {
        return startResponse.getBody().path("result").path("focusId").longValue();
    }

    protected void seedMonthlyCaches(long userId, YearMonth month, String marker) {
        redis.opsForZSet().add(RedisStatsKeyUtil.monthlyBooksZsetKey(userId, month), marker, 1);
        redis.opsForZSet().add(RedisStatsKeyUtil.monthlyFocusDailyKey(userId, month), marker, 1);
        redis.opsForZSet().add(RedisStatsKeyUtil.monthlyFocusHourlyKey(userId, month), marker, 1);
        redis.opsForValue().set(RedisStatsKeyUtil.monthlyBooksTotalKey(userId, month), marker);
        redis.opsForValue().set(RedisStatsKeyUtil.monthlyBooksExistsKey(userId, month), marker);
        redis.opsForValue().set(RedisStatsKeyUtil.monthlyFocusDailyExistsKey(userId, month), marker);
    }

    protected Map<String, String> monthlyCacheValues(long userId, YearMonth month, String marker) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(RedisStatsKeyUtil.monthlyBooksZsetKey(userId, month), marker);
        values.put(RedisStatsKeyUtil.monthlyBooksTotalKey(userId, month), marker);
        values.put(RedisStatsKeyUtil.monthlyBooksExistsKey(userId, month), marker);
        values.put(RedisStatsKeyUtil.monthlyFocusDailyKey(userId, month), marker);
        values.put(RedisStatsKeyUtil.monthlyFocusDailyExistsKey(userId, month), marker);
        values.put(RedisStatsKeyUtil.monthlyFocusHourlyKey(userId, month), marker);
        return values;
    }

    protected void assertMonthlyCachesPresent(long userId, YearMonth month, String marker) {
        org.assertj.core.api.Assertions.assertThat(redis.opsForZSet().score(
                RedisStatsKeyUtil.monthlyBooksZsetKey(userId, month), marker)).isEqualTo(1.0);
        org.assertj.core.api.Assertions.assertThat(redis.opsForValue().get(
                RedisStatsKeyUtil.monthlyBooksTotalKey(userId, month))).isEqualTo(marker);
        org.assertj.core.api.Assertions.assertThat(redis.opsForValue().get(
                RedisStatsKeyUtil.monthlyBooksExistsKey(userId, month))).isEqualTo(marker);
        org.assertj.core.api.Assertions.assertThat(redis.opsForZSet().score(
                RedisStatsKeyUtil.monthlyFocusDailyKey(userId, month), marker)).isEqualTo(1.0);
        org.assertj.core.api.Assertions.assertThat(redis.opsForValue().get(
                RedisStatsKeyUtil.monthlyFocusDailyExistsKey(userId, month))).isEqualTo(marker);
        org.assertj.core.api.Assertions.assertThat(redis.opsForZSet().score(
                RedisStatsKeyUtil.monthlyFocusHourlyKey(userId, month), marker)).isEqualTo(1.0);
    }

    protected PersistenceSnapshot snapshot(long libraryId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Library library = libraryRepository.findById(libraryId).orElseThrow();
            List<FocusRow> focuses = focusRepository.findAllByLibraryIdAndLibraryUserId(
                            libraryId, library.getUser().getId()).stream()
                    .sorted(Comparator.comparing(Focus::getStartedAt))
                    .map(focus -> new FocusRow(
                            focus.getId(), focus.getLibrary().getId(), focus.getTheme().getId(),
                            focus.getFocusDate(), focus.getStartedTime(), focus.getEndedTime(),
                            focus.getStartedAt(), focus.getEndedAt(), focus.getDurationSec(), focus.getEndPage()))
                    .toList();
            List<TimelineRow> timelines = timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library)
                    .stream()
                    .sorted(Comparator.comparing(Timeline::getOccurredAt))
                    .map(timeline -> new TimelineRow(
                            timeline.getType().name(), timeline.getTargetId(), timeline.getOccurredAt()))
                    .toList();
            return new PersistenceSnapshot(
                    focuses, timelines, library.getStartedAt(), library.getEndedAt(),
                    library.getFocusSec(), library.getPage(),
                    library.getReadingStatus().name(), focusRepository.countByLibraryAndEndedAtIsNotNull(library));
        });
    }

    record Fixture(User user, Library library, Theme theme, String accessToken) {}
    record FocusRow(Long id, Long libraryId, Long themeId, LocalDate focusDate,
                    LocalTime startedTime, LocalTime endedTime, LocalDateTime startedAt,
                    LocalDateTime endedAt, Integer durationSec, Integer endPage) {}
    record TimelineRow(String type, Long targetId, LocalDateTime occurredAt) {}
    record PersistenceSnapshot(List<FocusRow> focuses, List<TimelineRow> timelines,
                               LocalDate startedAt, LocalDate endedAt, long focusSec, int page,
                               String status, int completedCount) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfig {
        @Bean
        @Primary
        MutableKstClock mutableKstClock() {
            return new MutableKstClock(STARTED_AT);
        }
    }
}
