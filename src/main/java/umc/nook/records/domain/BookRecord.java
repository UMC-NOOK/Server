package umc.nook.records.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;
import umc.nook.bookshelves.domain.UserBookShelf;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "book_record")
public class BookRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @Column(name = "page")
    private String page;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshelf_id", nullable = false)
    private UserBookShelf bookshelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BookRecord parent;  // null이면 독립 레코드, 있으면 감상(comment)

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private RecordType recordType;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<BookRecord> comments = new ArrayList<>();
    public void updateRecord(String page, String content) {
        this.page = page;
        this.content = content;
    }

    public void updateCommentary(String content) {
        this.content = content;
    }
}
