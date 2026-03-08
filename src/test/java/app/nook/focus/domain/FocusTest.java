package app.nook.focus.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FocusTest {

    @Test
    @DisplayName("startFocus는 시작 시각과 focusDate를 설정한다")
    void startFocus_setsStartFields() {
        Focus focus = new Focus();

        focus.startFocus();

        assertThat(focus.getStartedAt()).isNotNull();
        assertThat(focus.getStartedTime()).isNotNull();
        assertThat(focus.getFocusDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("endFocus는 종료 시각과 durationSec를 계산한다")
    void endFocus_setsEndFieldsAndDuration() {
        Focus focus = new Focus();
        LocalDateTime startedAt = LocalDateTime.now().minusSeconds(90);
        ReflectionTestUtils.setField(focus, "startedAt", startedAt);
        ReflectionTestUtils.setField(focus, "focusDate", null);
        ReflectionTestUtils.setField(focus, "startedTime", null);

        focus.endFocus();

        assertThat(focus.getEndedAt()).isNotNull();
        assertThat(focus.getEndedTime()).isNotNull();
        assertThat(focus.getFocusDate()).isNotNull();
        assertThat(focus.getStartedTime()).isNotNull();
        assertThat(focus.getDurationSec()).isBetween(89, 91);
    }
}
