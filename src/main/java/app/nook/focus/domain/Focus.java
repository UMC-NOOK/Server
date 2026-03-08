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
    public Focus(Library library, Theme theme) {
        this.library = library;
        this.theme = theme;
    }

    public void startFocus() {
        LocalDateTime now = LocalDateTime.now();
        this.startedAt = now;
        this.focusDate = now.toLocalDate();
        this.startedTime = now.toLocalTime();
    }


    public void endFocus() {
        LocalDateTime now = LocalDateTime.now();
        this.endedAt = now;
        this.endedTime = now.toLocalTime();
        if (this.focusDate == null) {
            this.focusDate = now.toLocalDate();
        }
        if (this.startedTime == null && this.startedAt != null) {
            this.startedTime = this.startedAt.toLocalTime();
        }

        if (startedAt != null) {
            this.durationSec =
                    (int) java.time.Duration.between(startedAt, endedAt).getSeconds();
        }
    }
}
