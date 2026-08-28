package app.nook.focus.integration;

import app.nook.NookApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FocusHttpIntegrationSupport.MutableClockConfig.class)
class FocusMidnightHttpIntegrationTest extends FocusHttpIntegrationSupport {

    @Test
    void endFocus_whenKstSessionCrossesMidnight_persistsDailyRowsAndEvictsOnlyAffectedCaches() {
        Fixture fixture = fixture();
        ResponseEntity<JsonNode> startResponse = start(fixture);
        long originalFocusId = focusId(startResponse);
        seedMonthlyCaches(fixture.user().getId(), AFFECTED_MONTH, "affected");
        seedMonthlyCaches(fixture.user().getId(), UNRELATED_MONTH, "unrelated-month");
        seedMonthlyCaches(fixture.user().getId() + 1, AFFECTED_MONTH, "unrelated-user");

        clock.set(ENDED_AT);
        ResponseEntity<JsonNode> endResponse = end(originalFocusId, fixture.accessToken());

        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody().path("code").asText()).isEqualTo("SUCCESS-201");
        JsonNode result = endResponse.getBody().path("result");
        assertThat(endResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.isObject()).isTrue();
        assertThat(result.path("focusId").longValue()).isEqualTo(originalFocusId);
        assertThat(result.path("libraryId").longValue()).isEqualTo(fixture.library().getId());
        assertThat(result.path("startedAt").asText()).isEqualTo("2026-08-01T23:55:00");
        assertThat(result.path("endedAt").asText()).isEqualTo("2026-08-02T00:10:00");
        assertThat(result.path("durationSec").intValue()).isEqualTo(900);
        assertThat(result.path("durationText").asText()).isEqualTo("00:15:00");
        assertThat(result.path("page").intValue()).isEqualTo(72);
        assertThat(result.path("totalFocusSec").longValue()).isEqualTo(900L);
        assertThat(result.path("readingStatus").asText()).isEqualTo("FINISHED");

        PersistenceSnapshot snapshot = snapshot(fixture.library().getId());
        assertCompletedState(snapshot, fixture, originalFocusId);

        assertThat(monthlyCacheValues(fixture.user().getId(), AFFECTED_MONTH, "affected").keySet())
                .allSatisfy(key -> assertThat(redis.hasKey(key)).isFalse());
        assertMonthlyCachesPresent(fixture.user().getId(), UNRELATED_MONTH, "unrelated-month");
        assertMonthlyCachesPresent(fixture.user().getId() + 1, AFFECTED_MONTH, "unrelated-user");
    }

    @Test
    void endFocus_whenTwoHttpRequestsRace_returnsOneSuccessAndOneAlreadyEndedWithoutDuplicates() throws Exception {
        Fixture fixture = fixture();
        long originalFocusId = focusId(start(fixture));
        seedMonthlyCaches(fixture.user().getId(), AFFECTED_MONTH, "affected");
        seedMonthlyCaches(fixture.user().getId(), UNRELATED_MONTH, "unrelated");
        clock.set(ENDED_AT);
        CyclicBarrier startBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        List<ResponseEntity<JsonNode>> responses;
        try {
            Future<ResponseEntity<JsonNode>> first = executor.submit(() -> {
                startBarrier.await();
                return end(originalFocusId, fixture.accessToken());
            });
            Future<ResponseEntity<JsonNode>> second = executor.submit(() -> {
                startBarrier.await();
                return end(originalFocusId, fixture.accessToken());
            });
            responses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertThat(responses).extracting(ResponseEntity::getStatusCode)
                .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        ResponseEntity<JsonNode> conflict = responses.stream()
                .filter(response -> response.getStatusCode().equals(HttpStatus.CONFLICT))
                .findFirst().orElseThrow();
        assertThat(conflict.getBody().path("code").asText()).isEqualTo("FOCUS-003");

        ResponseEntity<JsonNode> retryResponse = end(originalFocusId, fixture.accessToken());
        assertThat(retryResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(retryResponse.getBody().path("code").asText()).isEqualTo("FOCUS-003");

        PersistenceSnapshot snapshot = snapshot(fixture.library().getId());
        assertCompletedState(snapshot, fixture, originalFocusId);
        assertThat(monthlyCacheValues(fixture.user().getId(), AFFECTED_MONTH, "affected").keySet())
                .allSatisfy(key -> assertThat(redis.hasKey(key)).isFalse());
        assertMonthlyCachesPresent(fixture.user().getId(), UNRELATED_MONTH, "unrelated");
    }

    private void assertCompletedState(PersistenceSnapshot snapshot, Fixture fixture, long originalFocusId) {
        assertThat(snapshot.focuses()).hasSize(2);
        long splitFocusId = snapshot.focuses().get(1).id();
        assertThat(splitFocusId).isNotEqualTo(originalFocusId);
        assertThat(snapshot.focuses()).containsExactly(
                new FocusRow(originalFocusId, fixture.library().getId(), fixture.theme().getId(),
                        LocalDate.of(2026, 8, 1), LocalTime.of(23, 55), LocalTime.MIDNIGHT,
                        STARTED_AT, LocalDateTime.of(2026, 8, 2, 0, 0), 300, null),
                new FocusRow(splitFocusId, fixture.library().getId(), fixture.theme().getId(),
                        LocalDate.of(2026, 8, 2), LocalTime.MIDNIGHT, LocalTime.of(0, 10),
                        LocalDateTime.of(2026, 8, 2, 0, 0), ENDED_AT, 600, 72)
        );
        assertThat(snapshot.timelines()).containsExactly(
                new TimelineRow("FOCUS", originalFocusId, STARTED_AT),
                new TimelineRow("FOCUS", splitFocusId, LocalDateTime.of(2026, 8, 2, 0, 0))
        );
        assertThat(snapshot.startedAt()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(snapshot.endedAt()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(snapshot.focusSec()).isEqualTo(900L);
        assertThat(snapshot.page()).isEqualTo(72);
        assertThat(snapshot.status()).isEqualTo("FINISHED");
        assertThat(snapshot.completedCount()).isEqualTo(2);
    }
}
