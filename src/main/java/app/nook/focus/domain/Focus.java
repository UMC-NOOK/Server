package app.nook.focus.domain;

import app.nook.global.common.BaseEntity;
import app.nook.library.domain.Library;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Library library;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "end_page")
    private Integer endPage;

    @Column(name = "focus_date")
    private LocalDate focusDate;

    @Column(name = "started_time")
    private LocalTime startedTime;

    @Column(name = "ended_time")
    private LocalTime endedTime;

    @Builder
    public Focus(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer durationSec,
            Integer endPage,
            Library library
    ) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSec = durationSec;
        this.endPage = endPage;
        this.focusDate = startedAt != null ? startedAt.toLocalDate() : null;
        this.startedTime = startedAt != null ? startedAt.toLocalTime() : null;
        this.endedTime = endedAt != null ? endedAt.toLocalTime() : null;
        this.library = library;
    }

    public void endFocus(LocalDateTime endedAt) {
        endFocus(endedAt, null);
    }

    public void endFocus(LocalDateTime endedAt, Integer endPage) {
        this.endedAt = endedAt;
        this.endPage = endPage;
        this.endedTime = endedAt.toLocalTime();
        this.durationSec = (int) Duration.between(this.startedAt, endedAt).getSeconds();
    }

    public void completeSegment(LocalDateTime startedAt, LocalDateTime endedAt, Integer endPage) {
        this.startedAt = startedAt;
        this.focusDate = startedAt.toLocalDate();
        this.startedTime = startedAt.toLocalTime();
        this.endedAt = endedAt;
        this.endedTime = endedAt.toLocalTime();
        this.durationSec = Math.toIntExact(Duration.between(startedAt, endedAt).getSeconds());
        this.endPage = endPage;
    }
}
