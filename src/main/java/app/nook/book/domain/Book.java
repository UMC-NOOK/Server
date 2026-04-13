package app.nook.book.domain;

import app.nook.book.domain.enums.SourceType;
import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.utils.BookUtils;
import app.nook.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(length = 13)
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

    @Column(name = "cover_image_key", length = 2000)
    private String coverImageKey;

    @Column(columnDefinition = "TEXT")
    private String aladinLink;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    // USER 도서 소유자 추적(ALADIN은 null)
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder
    public Book(
            String isbn13,
            String title,
            String author,
            String publisher,
            String publicationDate,
            Integer pages,
            String description,
            String coverImageKey,
            String aladinLink,
            SourceType sourceType,
            Long createdByUserId,
            Category category
    ) {
        this.isbn13 = isbn13;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
        this.pages = pages;
        this.description = description;
        this.coverImageKey = coverImageKey;
        this.aladinLink = aladinLink;
        this.sourceType = sourceType;
        this.createdByUserId = createdByUserId;
        this.category = category;
    }

    public void updateInfo(BookResponseDto.BookDetailDto info, Category category) {
        this.title = info.getTitle();
        this.author = info.getAuthor();
        this.publisher = info.getPublisher();
        this.publicationDate = info.getPublicationDate();
        this.pages = info.getPages();
        this.category = category;
        this.description = info.getDescription();
        this.coverImageKey = info.getCoverImageUrl();
        this.aladinLink = info.getAladinLink();
    }

    public void updateUserBookInfo(
            BookRequestDto.UpdateUserBookRequest request, String coverImageKey, Category category) {
        this.isbn13 = BookUtils.normalizeIsbn(request.isbn13());
        this.title = request.title();
        this.author = request.author();
        this.publisher = request.publisher();
        this.publicationDate = request.publicationDate();
        this.pages = request.pages();
        this.description = request.description();
        this.category = category;
        if (coverImageKey != null && !coverImageKey.isBlank()) {
            this.coverImageKey = coverImageKey;
        }
    }
}
