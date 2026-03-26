package app.nook.record.domain;

import app.nook.global.common.BaseEntity;
import app.nook.library.domain.Library;
import app.nook.record.domain.enums.Emotion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "records",
        indexes = {
                @Index(
                        name = "idx_records_library_id",
                        columnList = "emotion, library_id DESC"
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Record extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Enumerated(EnumType.STRING)
    private Emotion emotion;

    private String content;

    @OneToMany(mappedBy = "record", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("orderIndex ASC, createdDate ASC")
    private List<RecordImage> images = new ArrayList<>();

    @Builder
    public Record(Library library, Emotion emotion, String content) {
        this.library = library;
        this.emotion = emotion;
        this.content = content;
    }

    public void update(String content, Emotion emotion) {
        this.content = content;
        this.emotion = emotion;

    }
}
