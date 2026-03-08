package app.nook.focus.domain;

import app.nook.global.common.BaseEntity;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.library.domain.Library;
import jakarta.persistence.*;
import lombok.*;

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
    public Focus(Theme theme, LocalDateTime startedAt, LocalDateTime endedAt, Integer durationSec) {
        this.theme = theme;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSec = durationSec;
        this.focusDate = startedAt.toLocalDate();
    }
}
