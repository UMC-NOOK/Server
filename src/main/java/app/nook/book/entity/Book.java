package app.nook.book.entity;

import app.nook.book.dto.BookResponseDto;
import app.nook.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(length = 13, nullable = false, unique = true)
    private String isbn13;

    @Column(length = 1000)
    private String title;

    @Column(length = 1500)
    private String author;

    @Column(length = 1500)
    private String publisher;

    @Column(name = "publication_date")
    private String publicationDate;

    private Integer pages;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2000)
    private String coverImageUrl;

    @Column(columnDefinition = "TEXT")
    private String aladinLink;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public void updateInfo(BookResponseDto.BookDetailDto info, Category category) {
        this.title = info.getTitle();
        this.author = info.getAuthor();
        this.publisher = info.getPublisher();
        this.publicationDate = info.getPublicationDate();
        this.pages = info.getPages();
        this.category = category;
        this.description = info.getDescription();
        this.coverImageUrl = info.getCoverImageUrl();
        this.aladinLink = info.getAladinLink();
    }
}
