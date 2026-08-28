package app.nook.focus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

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
    @DisplayName("완료된 포커스는 저장용 날짜별 전체 초 구간으로 분할한다")
    void splitCompletedForPersistence_splitsAcrossMidnightIntoNormalizedSegments() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 55);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 10);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 8, 1),
                        LocalDateTime.of(2026, 8, 1, 23, 55),
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        300
                ),
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 8, 2),
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        LocalDateTime.of(2026, 8, 2, 0, 10),
                        600
                )
        );
    }

    @Test
    @DisplayName("정확히 자정에 끝난 완료 포커스는 이전 날짜 행 하나만 저장한다")
    void splitCompletedForPersistence_excludesNextDayWhenEndingAtExactMidnight() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 0);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(new FocusDailyTimeCalculator.CompletedFocusSegment(
                LocalDate.of(2026, 8, 1),
                startedAt,
                endedAt,
                3600
        ));
    }

    @Test
    @DisplayName("정확히 자정에 시작한 완료 포커스는 시작 날짜 행에 저장한다")
    void splitCompletedForPersistence_includesOnlyStartDayWhenStartingAtExactMidnight() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 30);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(new FocusDailyTimeCalculator.CompletedFocusSegment(
                LocalDate.of(2026, 8, 2),
                startedAt,
                endedAt,
                1800
        ));
    }

    @Test
    @DisplayName("월말과 연말을 넘는 완료 포커스는 중간 날짜 전체를 하나의 행으로 저장한다")
    void splitCompletedForPersistence_splitsMonthAndYearBoundariesWithFullMiddleDay() {
        LocalDateTime startedAt = LocalDateTime.of(2025, 12, 31, 23, 30);
        LocalDateTime endedAt = LocalDateTime.of(2026, 1, 2, 0, 30);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2025, 12, 31),
                        LocalDateTime.of(2025, 12, 31, 23, 30),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        1800
                ),
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 1, 1),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        86400
                ),
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 1, 2),
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 0, 30),
                        1800
                )
        );
    }

    @Test
    @DisplayName("자정을 넘는 나노초 구간은 전체 초로 정규화한 첫날 1초 행 하나를 저장한다")
    void splitCompletedForPersistence_normalizesFractionalMidnightIntervalToOneFirstDaySecond() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 59, 59, 500_000_000);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 0, 0, 500_000_000);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(new FocusDailyTimeCalculator.CompletedFocusSegment(
                LocalDate.of(2026, 8, 1),
                LocalDateTime.of(2026, 8, 1, 23, 59, 59),
                LocalDateTime.of(2026, 8, 2, 0, 0),
                1
        ));
    }

    @Test
    @DisplayName("같은 전체 초로 정규화되는 완료 포커스는 저장 행을 만들지 않는다")
    void splitCompletedForPersistence_returnsEmptyWhenEndpointsNormalizeToSameSecond() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 12, 0, 0, 100_000_000);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 1, 12, 0, 0, 900_000_000);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("저장용 완료 포커스 구간은 날짜 순서와 정확한 경계를 유지하고 전체 초 합계를 보존한다")
    void splitCompletedForPersistence_ordersExactBoundariesAndPreservesNormalizedDurationSum() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 31, 23, 30, 15, 900_000_000);
        LocalDateTime endedAt = LocalDateTime.of(2026, 9, 2, 0, 45, 30, 100_000_000);

        List<FocusDailyTimeCalculator.CompletedFocusSegment> result = calculator.splitCompletedForPersistence(startedAt, endedAt);

        assertThat(result).containsExactly(
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 8, 31),
                        LocalDateTime.of(2026, 8, 31, 23, 30, 15),
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        1785
                ),
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 9, 1),
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        LocalDateTime.of(2026, 9, 2, 0, 0),
                        86400
                ),
                new FocusDailyTimeCalculator.CompletedFocusSegment(
                        LocalDate.of(2026, 9, 2),
                        LocalDateTime.of(2026, 9, 2, 0, 0),
                        LocalDateTime.of(2026, 9, 2, 0, 45, 30),
                        2730
                )
        );
        assertThat(result.stream().mapToLong(FocusDailyTimeCalculator.CompletedFocusSegment::durationSec).sum())
                .isEqualTo(Duration.between(
                        startedAt.truncatedTo(ChronoUnit.SECONDS),
                        endedAt.truncatedTo(ChronoUnit.SECONDS)
                ).getSeconds());
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

    @Test
    void affectedYearMonths_sameMonth() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 1, 15, 11, 0);

        assertThat(calculator.affectedYearMonths(startedAt, endedAt, endedAt))
                .containsExactly(YearMonth.of(2026, 1));
    }

    @Test
    void affectedYearMonths_crossMonth() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 31, 23, 30);
        LocalDateTime endedAt = LocalDateTime.of(2026, 2, 1, 0, 30);

        assertThat(calculator.affectedYearMonths(startedAt, endedAt, endedAt))
                .containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 2));
    }

    @Test
    void affectedYearMonths_exactMidnightExcludesNextMonth() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 31, 23, 30);
        LocalDateTime endedAt = LocalDateTime.of(2026, 2, 1, 0, 0);

        assertThat(calculator.affectedYearMonths(startedAt, endedAt, endedAt))
                .containsExactly(YearMonth.of(2026, 1));
    }

    @Test
    void affectedYearMonths_multiMonth() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 4, 2, 10, 0);

        assertThat(calculator.affectedYearMonths(startedAt, endedAt, endedAt))
                .containsExactly(
                        YearMonth.of(2026, 1),
                        YearMonth.of(2026, 2),
                        YearMonth.of(2026, 3),
                        YearMonth.of(2026, 4)
                );
    }

    @Test
    void affectedYearMonths_ongoingUsesServerNow() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 31, 23, 30);
        LocalDateTime serverNow = LocalDateTime.of(2026, 2, 1, 0, 30);

        assertThat(calculator.affectedYearMonths(startedAt, null, serverNow))
                .containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 2));
    }

    @Test
    void affectedYearMonths_nonPositiveIntervalReturnsEmpty() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime sameTime = startedAt;
        LocalDateTime earlier = startedAt.minusMinutes(1);

        Set<YearMonth> zeroDuration = calculator.affectedYearMonths(startedAt, sameTime, sameTime);
        Set<YearMonth> negativeDuration = calculator.affectedYearMonths(startedAt, earlier, sameTime);

        assertThat(zeroDuration).isEmpty();
        assertThat(negativeDuration).isEmpty();
    }
}
