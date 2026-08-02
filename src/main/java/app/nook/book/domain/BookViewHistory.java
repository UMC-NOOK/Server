package app.nook.book.domain;

import app.nook.global.common.BaseEntity;
import app.nook.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "book_view_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_book_view_history_user_book", columnNames = {"user_id", "book_id"})
        },
        indexes = {
                @Index(name = "idx_book_view_history_user_modified",
                        columnList = "user_id, modified_date DESC, id DESC")
        }
)
public class BookViewHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @Builder
    public BookViewHistory(User user, Book book) {
        this.user = user;
        this.book = book;
    }
}
