package app.nook.timeline.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.repository.RecordRepository;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "server.address=127.0.0.1"})
@ActiveProfiles("test")
@Testcontainers
@Import(TimelineActionsIntegrationTest.TimeConfig.class)
@DisplayName("타임라인 연동 삭제 HTTP 통합 테스트")
class TimelineActionsIntegrationTest extends AbstractPostgresContainerTests {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @TestConfiguration
    static class TimeConfig {
        @Bean
        AtomicReference<Instant> testTime() {
            return new AtomicReference<>(Instant.parse("2026-08-31T14:00:00Z"));
        }

        @Bean
        @Primary
        Clock testClock(AtomicReference<Instant> testTime) {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return ZoneId.of("Asia/Seoul");
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return Clock.fixed(instant(), zone);
                }

                @Override
                public Instant instant() {
                    return testTime.get();
                }
            };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private FocusRepository focusRepository;

    @Autowired
    private TimelineRepository timelineRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private TimelineCommandService timelineCommandService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AtomicReference<Instant> testTime;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private User user;
    private Library library;
    private String token;

    @BeforeEach
    void setUp() {
        String id = UUID.randomUUID().toString();
        user = userRepository.save(User.builder()
                .email(id + "@example.test")
                .nickName("HTTP")
                .provider("GOOGLE")
                .providerId(id)
                .role(UserRole.USER)
                .build());
        Book book = bookRepository.save(Book.builder()
                .title("HTTP fixture")
                .author("fixture")
                .sourceType(SourceType.ALADIN)
                .build());
        library = libraryRepository.save(Library.builder()
                .user(user)
                .book(book)
                .build());
        token = jwtProvider.createAccessToken(user);
    }

    @Test
    @DisplayName("자정 분할 세션 삭제가 시간·캐시·타임라인에 반영되고 상세 원본 ID를 반환한다")
    void deleteFocus_분할세션_HTTP() throws Exception {
        Focus other = completedFocus();
        Timeline otherTimeline = appendTimeline(TimelineType.FOCUS, other.getId());
        Timeline registered = appendTimeline(TimelineType.REGISTER, library.getId());
        Timeline status = appendTimeline(TimelineType.STATUS, library.getId());
        Record record = recordRepository.save(Record.create(library, Emotion.FUN, "기록"));
        Timeline recordTimeline = appendTimeline(TimelineType.RECORD, record.getId());
        for (Timeline item : List.of(otherTimeline, registered, status, recordTimeline)) {
            JsonNode detail = response("GET", "/api/v1/library/" + library.getId() + "/timeline/" + item.getId(), null);
            assertThat(detail.path("targetId").asLong()).isEqualTo(item.getTargetId());
        }

        testTime.set(Instant.parse("2026-08-31T14:00:00Z"));
        long focusId = response("POST", "/api/v1/focuses/start", "{\"bookId\":" + library.getBook().getId() + "}")
                .path("focusId").asLong();
        assertThat(send("DELETE", "/api/v1/focuses/" + focusId, null).statusCode()).isEqualTo(409);
        testTime.set(Instant.parse("2026-08-31T15:30:00Z"));
        response("POST", "/api/v1/focuses/end", "{\"focusId\":" + focusId + ",\"page\":72,\"isFinished\":true}");
        Focus first = focusRepository.findById(focusId).orElseThrow();
        List<Focus> session = focusRepository.findByLibraryAndSessionIdOrderByIdAsc(library, first.getSessionId());
        assertThat(session).hasSize(2);
        assertThat(session).extracting(Focus::getSessionId).containsOnly(first.getSessionId());
        assertThat(first.getSessionId()).isNotEqualTo(other.getSessionId());
        assertThat(monthly("2026-08")).isEqualTo(60);
        assertThat(monthly("2026-09")).isEqualTo(31);

        response("DELETE", "/api/v1/focuses/" + session.get(1).getId(), null);

        assertThat(focusRepository.findByLibraryAndSessionIdOrderByIdAsc(library, first.getSessionId())).isEmpty();
        assertThat(focusRepository.existsById(other.getId())).isTrue();
        Library remaining = libraryRepository.findById(library.getId()).orElseThrow();
        assertThat(remaining.getFocusSec()).isEqualTo(60L);
        assertThat(remaining.getPage()).isEqualTo(72);
        assertThat(remaining.getReadingStatus()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library,
                PageRequest.of(0, 20)))
                .extracting(Timeline::getId)
                .containsExactlyInAnyOrder(otherTimeline.getId(), registered.getId(), status.getId(), recordTimeline.getId());
        assertThat(monthly("2026-08")).isZero();
        assertThat(monthly("2026-09")).isEqualTo(1);
        assertThat(send("DELETE", "/api/v1/focuses/" + focusId, null).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("같은 세션의 서로 다른 구간을 동시 삭제해도 한 번만 차감한다")
    void deleteFocus_동시삭제() throws Exception {
        Focus focus = completedFocus();
        Focus second = focusRepository.save(Focus.builder()
                .library(library)
                .sessionId(focus.getSessionId())
                .startedAt(LocalDateTime.of(2026, 9, 2, 0, 0))
                .endedAt(LocalDateTime.of(2026, 9, 2, 0, 1))
                .durationSec(60)
                .build());
        library.recordFocus(60);
        library = libraryRepository.save(library);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        try {
            List<Future<Integer>> results = transactionTemplate.execute(tx -> {
                libraryRepository.findByIdForUpdate(library.getId()).orElseThrow();
                List<Future<Integer>> pending = List.of(
                        workers.submit(() -> {
                            ready.countDown();
                            return send("DELETE", "/api/v1/focuses/" + focus.getId(), null).statusCode();
                        }),
                        workers.submit(() -> {
                            ready.countDown();
                            return send("DELETE", "/api/v1/focuses/" + second.getId(), null).statusCode();
                        }));
                await(ready);
                return pending;
            });
            assertThat(List.of(results.get(0).get(20, TimeUnit.SECONDS), results.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 404);
            assertThat(libraryRepository.findById(library.getId()).orElseThrow().getFocusSec()).isZero();
        } finally {
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("삭제와 타임라인 생성이 겹쳐도 삭제된 포커스의 이력이 남지 않는다")
    void deleteFocus_타임라인생성경합() throws Exception {
        Focus focus = completedFocus();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        try {
            List<Future<?>> results = transactionTemplate.execute(tx -> {
                libraryRepository.findByIdForUpdate(library.getId()).orElseThrow();
                List<Future<?>> pending = List.of(
                        workers.submit(() -> {
                            ready.countDown();
                            timelineCommandService.appendFocusCompleted(focus.getId());
                        }),
                        workers.submit(() -> {
                            ready.countDown();
                            assertThat(send("DELETE", "/api/v1/focuses/" + focus.getId(), null).statusCode()).isEqualTo(200);
                            return null;
                        }));
                await(ready);
                return pending;
            });
            for (Future<?> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
            assertThat(focusRepository.existsById(focus.getId())).isFalse();
            assertThat(timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library)).isEmpty();
        } finally {
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("기록 삭제 시 연결된 타임라인만 삭제되고 상세 조회는 404를 반환한다")
    void deleteRecord_타임라인연동_HTTP() throws Exception {
        // given
        String recordsPath = "/api/v1/records";
        String timelinePath = "/api/v1/library/" + library.getId() + "/timeline";
        long recordId = response("POST", recordsPath + "/books/" + library.getBook().getId(),
                "{\"content\":\"삭제할 기록\",\"emotion\":\"FUN\",\"imageKeys\":[]}")
                .path("recordId").asLong();
        long otherRecordId = response("POST", recordsPath + "/books/" + library.getBook().getId(),
                "{\"content\":\"유지할 기록\",\"emotion\":\"FUN\",\"imageKeys\":[]}")
                .path("recordId").asLong();
        Timeline registered = appendTimeline(TimelineType.REGISTER, library.getId());
        List<Timeline> timelines = timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(
                library, PageRequest.of(0, 20));
        Timeline recordTimeline = timelines.stream()
                .filter(item -> item.getType() == TimelineType.RECORD && item.getTargetId().equals(recordId))
                .findFirst().orElseThrow();
        assertThat(response("GET", timelinePath + "/" + recordTimeline.getId(), null)
                .path("targetId").asLong()).isEqualTo(recordId);

        // when
        JsonNode deleted = response("DELETE", recordsPath + "/" + recordId, null);

        // then
        assertThat(deleted.path("recordId").asLong()).isEqualTo(recordId);
        assertThat(send("GET", recordsPath + "/" + recordId, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", timelinePath + "/" + recordTimeline.getId(), null).statusCode()).isEqualTo(404);
        assertThat(response("GET", timelinePath, null).findValuesAsText("timelineId"))
                .doesNotContain(recordTimeline.getId().toString())
                .hasSize(2);
        assertThat(response("GET", recordsPath + "/" + otherRecordId, null)
                .path("recordId").asLong()).isEqualTo(otherRecordId);
        assertThat(response("GET", timelinePath + "/" + registered.getId(), null)
                .path("targetId").asLong()).isEqualTo(library.getId());
        assertThat(recordRepository.existsById(recordId)).isFalse();
        assertThat(timelineRepository.existsById(recordTimeline.getId())).isFalse();
        assertThat(send("DELETE", recordsPath + "/" + recordId, null).statusCode()).isEqualTo(404);
    }

    private void await(CountDownLatch ready) {
        try {
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Focus completedFocus() {
        library.recordFocus(60);
        library = libraryRepository.save(library);
        return focusRepository.save(Focus.builder()
                .library(library)
                .startedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 9, 1, 10, 1))
                .durationSec(60)
                .build());
    }

    private Timeline appendTimeline(TimelineType type, Long targetId) {
        return timelineRepository.save(Timeline.builder()
                .library(library)
                .type(type)
                .targetId(targetId)
                .occurredAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .previewText("기록")
                .build());
    }

    private int monthly(String month) throws Exception {
        return response("GET", "/api/v1/library/stats/focus-monthly?yearMonth=" + month, null)
                .path("totalFocusMin").asInt();
    }

    private JsonNode response(String method, String path, String body) throws Exception {
        HttpResponse<String> result = send(method, path, body);
        assertThat(result.statusCode()).as(path + ": " + result.body()).isEqualTo(200);
        return objectMapper.readTree(result.body()).path("result");
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
