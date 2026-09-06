package app.nook.focus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FocusCompletionSegmenter 테스트")
class FocusCompletionSegmenterTest {

    private final FocusCompletionSegmenter segmenter = new FocusCompletionSegmenter();

    @Test
    @DisplayName("같은 날짜의 완료 구간은 하나의 초 단위 세그먼트로 반환한다")
    void 같은_날짜의_완료_구간은_하나의_초_단위_세그먼트로_반환한다() {
        // Given
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 10, 15, 30, 900_000_000);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 1, 11, 45, 31, 100_000_000);

        // When
        List<FocusCompletionSegmenter.CompletedFocusSegment> result = segmenter.split(startedAt, endedAt);

        // Then
        assertThat(result).containsExactly(new FocusCompletionSegmenter.CompletedFocusSegment(
                LocalDateTime.of(2026, 8, 1, 10, 15, 30),
                LocalDateTime.of(2026, 8, 1, 11, 45, 31),
                5_401
        ));
        assertPositiveDurationsAndNormalizedTotal(startedAt, endedAt, result);
    }

    @Test
    @DisplayName("자정을 넘는 완료 구간은 날짜별로 3600초와 1800초로 분할한다")
    void 자정을_넘는_완료_구간은_날짜별로_3600초와_1800초로_분할한다() {
        // Given
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 30);

        // When
        List<FocusCompletionSegmenter.CompletedFocusSegment> result = segmenter.split(startedAt, endedAt);

        // Then
        assertThat(result).containsExactly(
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 1, 23, 0),
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        3_600
                ),
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        LocalDateTime.of(2026, 8, 2, 0, 30),
                        1_800
                )
        );
        assertPositiveDurationsAndNormalizedTotal(startedAt, endedAt, result);
    }

    @Test
    @DisplayName("여러 날 완료 구간은 중간 날짜를 하루 전체 세그먼트로 분할한다")
    void 여러_날_완료_구간은_중간_날짜를_하루_전체_세그먼트로_분할한다() {
        // Given
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 4, 1, 0);

        // When
        List<FocusCompletionSegmenter.CompletedFocusSegment> result = segmenter.split(startedAt, endedAt);

        // Then
        assertThat(result).containsExactly(
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 1, 23, 0),
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        3_600
                ),
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        LocalDateTime.of(2026, 8, 3, 0, 0),
                        86_400
                ),
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 3, 0, 0),
                        LocalDateTime.of(2026, 8, 4, 0, 0),
                        86_400
                ),
                new FocusCompletionSegmenter.CompletedFocusSegment(
                        LocalDateTime.of(2026, 8, 4, 0, 0),
                        LocalDateTime.of(2026, 8, 4, 1, 0),
                        3_600
                )
        );
        assertPositiveDurationsAndNormalizedTotal(startedAt, endedAt, result);
    }

    @Test
    @DisplayName("초 단위 절삭 후 1초 미만인 완료 구간은 0초 세그먼트를 반환한다")
    void 초_단위_절삭_후_1초_미만인_완료_구간은_0초_세그먼트를_반환한다() {
        // Given
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 10, 0, 0, 100_000_000);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 1, 10, 0, 0, 900_000_000);

        // When
        List<FocusCompletionSegmenter.CompletedFocusSegment> result = segmenter.split(startedAt, endedAt);

        // Then
        assertThat(result).containsExactly(new FocusCompletionSegmenter.CompletedFocusSegment(
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                0
        ));
    }

    private void assertPositiveDurationsAndNormalizedTotal(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            List<FocusCompletionSegmenter.CompletedFocusSegment> segments
    ) {
        assertThat(segments).allSatisfy(segment -> assertThat(segment.durationSec()).isPositive());
        assertThat(segments.stream().mapToLong(FocusCompletionSegmenter.CompletedFocusSegment::durationSec).sum())
                .isEqualTo(Duration.between(
                        startedAt.truncatedTo(ChronoUnit.SECONDS),
                        endedAt.truncatedTo(ChronoUnit.SECONDS)
                ).getSeconds());
    }
}
