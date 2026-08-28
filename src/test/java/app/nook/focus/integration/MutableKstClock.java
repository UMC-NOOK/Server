package app.nook.focus.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class MutableKstClock extends Clock {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AtomicReference<Instant> instant;

    MutableKstClock(LocalDateTime initialTime) {
        this.instant = new AtomicReference<>(toInstant(initialTime));
    }

    void set(LocalDateTime time) {
        instant.set(toInstant(time));
    }

    @Override
    public ZoneId getZone() {
        return KST;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        return zone.equals(KST) ? this : Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

    private static Instant toInstant(LocalDateTime time) {
        return Objects.requireNonNull(time, "time").atZone(KST).toInstant();
    }
}
