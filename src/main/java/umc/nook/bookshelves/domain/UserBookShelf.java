package umc.nook.bookshelves.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.BaseTimeEntity;
import umc.nook.book.domain.Book;
import umc.nook.records.domain.BookRecord;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_bookshelf", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookShelf extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_status")
    private ReadingStatus readingStatus;

    @Column(name = "recorded_at")
    private LocalDate recordedAt;

    @OneToMany(mappedBy = "bookshelf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookRecord> records = new ArrayList<>();

    public void updateReadingStatus(ReadingStatus reading) {
        this.readingStatus = reading;
    }
}