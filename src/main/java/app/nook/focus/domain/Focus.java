package app.nook.focus.domain;

import app.nook.global.common.BaseEntity;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.library.domain.Library;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "focuses",
        indexes = {
                @Index(
                        name = "idx_focus_library_focus_date",
                        columnList = "library_id, focus_date"
                ),
        }
)
public class Focus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "focus_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "focus_date")
    private LocalDate focusDate;

    @Column(name = "started_time")
    private LocalTime startedTime;

    @Column(name = "ended_time")
    private LocalTime endedTime;

    @Builder
    public Focus(Theme theme, LocalDateTime startedAt, LocalDateTime endedAt, Integer durationSec, Library library) {
        this.theme = theme;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSec = resolveDurationSec(startedAt, endedAt, durationSec);
        this.focusDate = startedAt != null ? startedAt.toLocalDate() : null;
        this.startedTime = startedAt != null ? startedAt.toLocalTime() : null;
        this.endedTime = endedAt != null ? endedAt.toLocalTime() : null;
        this.library = library;
    }

    private Integer resolveDurationSec(LocalDateTime startedAt, LocalDateTime endedAt, Integer durationSec) {
        if (startedAt != null && endedAt != null) {
            long computedDurationSec = Duration.between(startedAt, endedAt).getSeconds();
            if (computedDurationSec < 0) {
                throw new IllegalArgumentException("endedAt must be after startedAt");
            }
            if (computedDurationSec > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("durationSec is out of range");
            }
            return (int) computedDurationSec;
        }

        if (durationSec != null && durationSec < 0) {
            throw new IllegalArgumentException("durationSec must not be negative");
        }

        return durationSec;
    }
}
