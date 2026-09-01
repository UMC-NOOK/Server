package app.nook.library.domain;

import app.nook.book.domain.Book;
import app.nook.global.common.BaseEntity;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "focus_sec")
    private Long focusSec = 0L;

    @Column(name = "page")
    private int page = 0;

    @Version
    private Long version;

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
    public void recordFocus(long addedSeconds) {
        this.focusSec += addedSeconds;
    }

    // 페이지 업데이트
    public void recordPage(int page) {
        this.page = page;
    }

    // 완독 -> 독서 중
    // 독서 중 -> 완독 :  endTime 업데이트
    public void updateStatus(ReadingStatus readingStatus) {
        updateStatus(readingStatus, LocalDateTime.now().toLocalDate());
    }

    public void updateStatus(ReadingStatus readingStatus, LocalDate statusDate) {
        this.readingStatus = readingStatus;
        if(readingStatus.equals(ReadingStatus.FINISHED)){
            this.endedAt = statusDate;
        } else if(readingStatus.equals(ReadingStatus.READING)){
            this.startedAt = statusDate;
        }
    }


}
