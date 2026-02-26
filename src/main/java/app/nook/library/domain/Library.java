package app.nook.library.domain;

import app.nook.book.domain.Book;
import app.nook.global.common.BaseEntity;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "library",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_library_user_book",
                        columnNames = {"user_id", "book_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_library_user_status_id",
                        columnList = "user_id, reading_status, library_id DESC"
                ),
        }
)
public class Library extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_status")
    private ReadingStatus readingStatus = ReadingStatus.BEFORE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "focus_min")
    private Long focusMin = 0L;

    @Builder
    public Library(
            User user,
            Book book
    ){
        this.user = user;
        this.book = book;
        this.startedAt = LocalDateTime.now().toLocalDate();
    }


    // 포커스 시 읽은 기록 업데이트
    public void recordFocus(long addedMinutes) {
        this.focusMin += addedMinutes;
    }

    // 완독 -> 독서 중
    // 독서 중 -> 완독 :  endTime 업데이트
    public void updateStatus(ReadingStatus readingStatus) {
        this.readingStatus = readingStatus;
        if(readingStatus.equals(ReadingStatus.FINISHED)){
            this.endedAt = LocalDateTime.now().toLocalDate();
        } else if(readingStatus.equals(ReadingStatus.READING)){
            this.startedAt = LocalDateTime.now().toLocalDate();
        }
    }


}
