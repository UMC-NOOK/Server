package app.nook.focus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FocusDailyTimeCalculator 테스트")
class FocusDailyTimeCalculatorTest {

    private final FocusDailyTimeCalculator calculator = new FocusDailyTimeCalculator();

    @Test
    @DisplayName("23시 55분부터 다음 날 0시 10분까지 날짜별로 300초와 600초를 계산한다")
    void splitAcrossMidnight() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 55);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 10);

        List<FocusDailyTimeCalculator.DailyFocusTime> result = calculator.splitByDate(
                startedAt,
                endedAt,
                endedAt,
                LocalDate.of(2026, 8, 1).atStartOfDay(),
                LocalDate.of(2026, 8, 3).atStartOfDay()
        );

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 8, 1), 300),
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 8, 2), 600)
        );
    }

    @Test
    @DisplayName("정확히 자정에 종료한 세션은 다음 날에 포함하지 않는다")
    void excludeNextDayWhenSessionEndsAtMidnight() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 0);

        assertThat(calculator.calculateForDate(startedAt, endedAt, endedAt, LocalDate.of(2026, 8, 1)))
                .isEqualTo(3600);
        assertThat(calculator.calculateForDate(startedAt, endedAt, endedAt, LocalDate.of(2026, 8, 2)))
                .isZero();
    }

    @Test
    @DisplayName("정확히 자정에 시작한 세션은 전날에 포함하지 않는다")
    void excludePreviousDayWhenSessionStartsAtMidnight() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 30);

        assertThat(calculator.calculateForDate(startedAt, endedAt, endedAt, LocalDate.of(2026, 8, 1)))
                .isZero();
        assertThat(calculator.calculateForDate(startedAt, endedAt, endedAt, LocalDate.of(2026, 8, 2)))
                .isEqualTo(1800);
    }

    @Test
    @DisplayName("월말과 연말을 포함한 여러 날짜 세션을 날짜별로 계산한다")
    void splitAcrossMonthAndYear() {
        LocalDateTime startedAt = LocalDateTime.of(2025, 12, 31, 23, 30);
        LocalDateTime endedAt = LocalDateTime.of(2026, 1, 2, 0, 30);

        List<FocusDailyTimeCalculator.DailyFocusTime> result = calculator.splitByDate(
                startedAt,
                endedAt,
                endedAt,
                LocalDate.of(2025, 12, 1).atStartOfDay(),
                LocalDate.of(2026, 2, 1).atStartOfDay()
        );

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2025, 12, 31), 1800),
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 1, 1), 86400),
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 1, 2), 1800)
        );
    }

    @Test
    @DisplayName("진행 중 세션은 서버 현재 시각을 종료 시각으로 사용한다")
    void useServerNowForInProgressFocus() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 50);
        LocalDateTime serverNow = LocalDateTime.of(2026, 8, 2, 0, 20);

        List<FocusDailyTimeCalculator.DailyFocusTime> result = calculator.splitByDate(
                startedAt,
                null,
                serverNow,
                LocalDate.of(2026, 8, 1).atStartOfDay(),
                LocalDate.of(2026, 8, 3).atStartOfDay()
        );

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 8, 1), 600),
                new FocusDailyTimeCalculator.DailyFocusTime(LocalDate.of(2026, 8, 2), 1200)
        );
    }

    @Test
    @DisplayName("조회 범위 밖의 세션 시간은 제외한다")
    void clipToRequestedRange() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 31, 23, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 9, 1, 1, 0);

        List<FocusDailyTimeCalculator.DailyFocusTime> result = calculator.splitByDate(
                startedAt,
                endedAt,
                endedAt,
                LocalDate.of(2026, 8, 1).atStartOfDay(),
                LocalDate.of(2026, 9, 1).atStartOfDay()
        );

        assertThat(result).hasSize(31);
        assertThat(result).allMatch(segment -> segment.durationSec() == 86400);
        assertThat(result).extracting(FocusDailyTimeCalculator.DailyFocusTime::date)
                .startsWith(LocalDate.of(2026, 8, 1))
                .endsWith(LocalDate.of(2026, 8, 31));
    }
}
