package app.nook.book.domain;

import app.nook.book.domain.enums.MallType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="category_id")
    private Long id;

    @Column(length = 50)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    private MallType mallType;

    private int aladinCategoryId;

    @OneToMany(mappedBy = "category")
    private List<Book> books = new ArrayList<>();

    @Builder
    public Category(String categoryName, MallType mallType, int aladinCategoryId) {
        this.categoryName = categoryName;
        this.mallType = mallType;
        this.aladinCategoryId = aladinCategoryId;
    }

    public static Category of(MallType mallType, String categoryName, int aladinCategoryId) {
        return Category.builder()
                .mallType(mallType)
                .aladinCategoryId(aladinCategoryId)
                .categoryName(categoryName)
                .build();
    }
}
