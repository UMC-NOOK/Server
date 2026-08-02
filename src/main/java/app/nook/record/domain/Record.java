package app.nook.record.domain;

import app.nook.global.common.BaseEntity;
import app.nook.library.domain.Library;
import app.nook.record.domain.enums.Emotion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Record extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Library library;

    @Enumerated(EnumType.STRING)
    private Emotion emotion;

    private String content;

    @OneToMany(mappedBy = "record")
    @OrderBy("orderIndex ASC, createdDate ASC")
    private List<RecordImage> images = new ArrayList<>();

    private Record(Library library, Emotion emotion, String content) {
        this.library = library;
        this.emotion = emotion;
        this.content = content;
    }

    public static Record create(Library library, Emotion emotion, String content) {
        return new Record(library, emotion, content);
    }

    public void update(String content, Emotion emotion) {
        this.content = content;
        this.emotion = emotion;
    }
}
