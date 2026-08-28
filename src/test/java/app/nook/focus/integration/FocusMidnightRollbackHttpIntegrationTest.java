package app.nook.focus.integration;

import app.nook.NookApplication;
import app.nook.timeline.service.TimelineCommandService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest(classes = NookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FocusHttpIntegrationSupport.MutableClockConfig.class)
class FocusMidnightRollbackHttpIntegrationTest extends FocusHttpIntegrationSupport {

    @MockitoSpyBean
    private TimelineCommandService timelineCommandService;

    @BeforeEach
    void resetTimelineSpy() {
        reset(timelineCommandService);
    }

    @Test
    void endFocus_whenSecondTimelineAppendFails_rollsBackDatabaseAndPreservesRedis() {
        Fixture fixture = fixture();
        long originalFocusId = focusId(start(fixture));
        seedMonthlyCaches(fixture.user().getId(), AFFECTED_MONTH, "affected");
        seedMonthlyCaches(fixture.user().getId(), UNRELATED_MONTH, "unrelated");
        AtomicInteger appendCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (appendCount.incrementAndGet() == 2) {
                throw new IllegalStateException("forced second timeline append failure");
            }
            return invocation.callRealMethod();
        }).when(timelineCommandService).appendFocusCompleted(any());

        clock.set(ENDED_AT);
        ResponseEntity<JsonNode> response = end(originalFocusId, fixture.accessToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().path("code").asText()).isEqualTo("COMMON-001");
        assertThat(appendCount).hasValue(2);
        PersistenceSnapshot snapshot = snapshot(fixture.library().getId());
        assertThat(snapshot.focuses()).containsExactly(
                new FocusRow(originalFocusId, fixture.library().getId(), fixture.theme().getId(),
                        LocalDate.of(2026, 8, 1), LocalTime.of(23, 55), null,
                        STARTED_AT, null, 0, null)
        );
        assertThat(snapshot.timelines()).isEmpty();
        assertThat(snapshot.focusSec()).isZero();
        assertThat(snapshot.page()).isZero();
        assertThat(snapshot.status()).isEqualTo("READING");
        assertThat(snapshot.completedCount()).isZero();
        assertMonthlyCachesPresent(fixture.user().getId(), AFFECTED_MONTH, "affected");
        assertMonthlyCachesPresent(fixture.user().getId(), UNRELATED_MONTH, "unrelated");
    }
}
