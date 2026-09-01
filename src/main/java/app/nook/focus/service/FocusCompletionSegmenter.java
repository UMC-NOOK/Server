package app.nook.focus.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class FocusCompletionSegmenter {

    public List<CompletedFocusSegment> split(LocalDateTime startedAt, LocalDateTime endedAt) {
        LocalDateTime normalizedStartedAt = startedAt.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime normalizedEndedAt = endedAt.truncatedTo(ChronoUnit.SECONDS);

        if (normalizedStartedAt.isAfter(normalizedEndedAt)) {
            return List.of();
        }
        if (normalizedStartedAt.equals(normalizedEndedAt)) {
            return List.of(new CompletedFocusSegment(
                    normalizedStartedAt,
                    normalizedEndedAt,
                    0
            ));
        }

        List<CompletedFocusSegment> segments = new ArrayList<>();
        LocalDateTime segmentStartedAt = normalizedStartedAt;

        while (segmentStartedAt.isBefore(normalizedEndedAt)) {
            LocalDateTime nextMidnight = segmentStartedAt.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEndedAt = normalizedEndedAt.isBefore(nextMidnight)
                    ? normalizedEndedAt
                    : nextMidnight;
            long durationSec = Duration.between(segmentStartedAt, segmentEndedAt).getSeconds();

            if (durationSec > 0) {
                segments.add(new CompletedFocusSegment(segmentStartedAt, segmentEndedAt, durationSec));
            }
            segmentStartedAt = segmentEndedAt;
        }

        return List.copyOf(segments);
    }

    public record CompletedFocusSegment(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            long durationSec
    ) {
    }
}
