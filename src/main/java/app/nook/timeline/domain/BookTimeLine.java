package app.nook.timeline.domain;

import app.nook.global.common.BaseEntity;
import app.nook.library.domain.Library;
import app.nook.timeline.domain.enums.BookTimeLineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "book_timelines",
        indexes = {
                @Index(
                        name = "idx_book_timelines_library_created",
                        columnList = "library_id, created_date DESC"
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

    @Column(name = "snapshot_value")
    private String snapshotValue;

    @Column(name = "target_id")
    private Long targetId;

    @Builder
    public BookTimeLine(
            Library library,
            BookTimeLineType type,
            String snapshotValue,
            Long targetId
    ) {
        this.library = library;
        this.type = type;
        this.snapshotValue = snapshotValue;
        this.targetId = targetId;
    }
}
