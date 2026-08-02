package app.nook.record.domain;


import app.nook.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "record_images")
public class RecordImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "record_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Record record;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    private String key;

    private Integer orderIndex;

    @Builder
    public RecordImage(Record record, String key, Integer orderIndex) {
        this.record = record;
        this.key = key;
        this.orderIndex = orderIndex;
    }
}
