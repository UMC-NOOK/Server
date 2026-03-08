package app.nook.focus.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FocusTest {

    @Test
    @DisplayName("빌더로 생성하면 startedAt 기준으로 focusDate가 설정된다")
    void builder_setsFocusDateFromStartedAt() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0);
        LocalDateTime endedAt = startedAt.plusMinutes(30);

        Focus focus = Focus.builder()
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(1800)
                .build();

        assertThat(focus.getStartedAt()).isEqualTo(startedAt);
        assertThat(focus.getEndedAt()).isEqualTo(endedAt);
        assertThat(focus.getDurationSec()).isEqualTo(1800);
        assertThat(focus.getFocusDate()).isEqualTo(LocalDate.of(2026, 3, 8));
    }

    @Test
    @DisplayName("focusDate는 startedAt의 날짜와 동일하다")
    void focusDate_equalsStartedAtDate() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 8, 23, 59, 0);

        Focus focus = Focus.builder()
                .startedAt(startedAt)
                .endedAt(startedAt.plusMinutes(2))
                .durationSec(120)
                .build();

        assertThat(focus.getFocusDate()).isEqualTo(startedAt.toLocalDate());
    }

    @Test
    @DisplayName("startedAt/endedAt가 있으면 durationSec는 시간차로 계산된다")
    void builder_computesDurationSecFromStartedAtAndEndedAt() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0);
        LocalDateTime endedAt = startedAt.plusMinutes(30);

        Focus focus = Focus.builder()
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(1)
                .build();

        assertThat(focus.getDurationSec()).isEqualTo(1800);
    }

    @Test
    @DisplayName("endedAt이 startedAt보다 빠르면 예외가 발생한다")
    void builder_throwsWhenEndedAtBeforeStartedAt() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0);
        LocalDateTime endedAt = startedAt.minusMinutes(1);

        assertThatThrownBy(() -> Focus.builder()
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(60)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endedAt must be after startedAt");
    }
}
