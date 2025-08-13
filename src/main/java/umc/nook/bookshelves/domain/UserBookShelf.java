package umc.nook.bookshelves.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import umc.nook.BaseTimeEntity;
import umc.nook.book.domain.Book;
import umc.nook.records.domain.BookRecord;
import umc.nook.records.domain.ChatRecord;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_bookshelf", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="reading_status", nullable=false)
    private ReadingStatus readingStatus;

    @NotNull
    @Column(name="recorded_at", nullable=false)
    private LocalDate recordedAt;

    @OneToMany(mappedBy = "bookshelf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookRecord> bookRecords = new ArrayList<>();

    @OneToMany(mappedBy = "bookshelf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRecord> chatRecords = new ArrayList<>();

    public void updateReadingStatus(ReadingStatus readingStatus) {
        this.readingStatus = readingStatus;
    }

    @Builder
    public UserBookShelf(User user, Book book, LocalDate recordedAt, ReadingStatus readingStatus ) {
        this.user = user;
        this.book = book;
        this.recordedAt = recordedAt;
        this.readingStatus = readingStatus;
    }

}