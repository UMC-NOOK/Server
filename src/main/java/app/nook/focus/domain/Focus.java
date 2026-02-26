package app.nook.focus.domain;

import app.nook.global.common.BaseEntity;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.library.domain.Library;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "focuses",
        indexes = {
                @Index(
                        name = "idx_focus_library_started",
                        columnList = "library_id, started_at"
                )
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

    @Builder
    public Focus(Library library, Theme theme) {
        this.library = library;
        this.theme = theme;
    }

    public void startFocus() {
        this.startedAt = LocalDateTime.now();
    }


    public void endFocus() {
        this.endedAt = LocalDateTime.now();

        if (startedAt != null) {
            this.durationSec =
                    (int) java.time.Duration.between(startedAt, endedAt).getSeconds();
        }
    }
}
