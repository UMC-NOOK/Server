package umc.nook.book.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(length = 13, nullable = false, unique = true)
    private String isbn13;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 255)
    private String publisher;

    private LocalDateTime publicationDate;

    private int pages;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255, nullable = false)
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
