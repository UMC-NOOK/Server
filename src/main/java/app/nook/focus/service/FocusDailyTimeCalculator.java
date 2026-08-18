package app.nook.focus.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FocusDailyTimeCalculator {

    public Set<YearMonth> affectedYearMonths(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime serverNow
    ) {
        LocalDateTime effectiveEnd = endedAt == null ? serverNow : endedAt;
        if (!startedAt.isBefore(effectiveEnd)) {
            return Set.of();
        }

        YearMonth firstMonth = YearMonth.from(startedAt);
        YearMonth lastMonth = YearMonth.from(effectiveEnd.minusNanos(1));
        Set<YearMonth> affectedMonths = new LinkedHashSet<>();
        for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            affectedMonths.add(month);
        }
        return Collections.unmodifiableSet(affectedMonths);
    }

    public List<DailyFocusTime> splitByDate(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime serverNow,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        LocalDateTime effectiveEnd = endedAt == null ? serverNow : endedAt;
        LocalDateTime segmentStart = laterOf(startedAt, rangeStart);
        LocalDateTime sessionEnd = earlierOf(effectiveEnd, rangeEnd);

        if (!segmentStart.isBefore(sessionEnd)) {
            return List.of();
        }

        List<DailyFocusTime> segments = new ArrayList<>();
        LocalDateTime cursor = segmentStart;

        while (cursor.isBefore(sessionEnd)) {
            LocalDate date = cursor.toLocalDate();
            LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = earlierOf(sessionEnd, nextDayStart);
            long durationSec = Duration.between(cursor, segmentEnd).getSeconds();

            if (durationSec > 0) {
                segments.add(new DailyFocusTime(date, durationSec));
            }
            cursor = segmentEnd;
        }

        return List.copyOf(segments);
    }

    public long calculateForDate(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime serverNow,
            LocalDate date
    ) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        return splitByDate(startedAt, endedAt, serverNow, dayStart, nextDayStart).stream()
                .mapToLong(DailyFocusTime::durationSec)
                .sum();
    }

    public boolean overlaps(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime serverNow,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        return !splitByDate(startedAt, endedAt, serverNow, rangeStart, rangeEnd).isEmpty();
    }

    private LocalDateTime laterOf(LocalDateTime first, LocalDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDateTime earlierOf(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    public record DailyFocusTime(LocalDate date, long durationSec) {
    }
}
