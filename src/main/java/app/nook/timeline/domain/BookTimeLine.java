package app.nook.timeline.domain;

import app.nook.global.common.BaseEntity;
import app.nook.library.domain.Library;
import app.nook.timeline.domain.enums.BookTimeLineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "book_timelines",
        indexes = {
                @Index(
                        name = "idx_book_timelines_library_occurred",
                        columnList = "library_id, occurred_at DESC, timeline_id DESC"
                ),
        }
)
public class BookTimeLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeline_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private BookTimeLineType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "preview_text", length = 500)
    private String previewText;

    @Builder
    public BookTimeLine(
            Library library,
            BookTimeLineType type,
            Long targetId,
            LocalDateTime occurredAt,
            String previewText
    ) {
        this.library = library;
        this.type = type;
        this.targetId = targetId;
        this.occurredAt = occurredAt;
        this.previewText = previewText;
    }
}
