package umc.nook.book.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(length = 13, nullable = false, unique = true)
    private String isbn13;

    @Column(length = 255)
    private String title;

    private String author;

    @Column(length = 255)
    private String publisher;

    private String publicationDate;

    private Integer pages;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
