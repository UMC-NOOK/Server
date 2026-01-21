package app.nook.book.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Builder.Default
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public static Category of(MallType mallType, int aladinCategoryId, String categoryName) {
        return Category.builder()
                .mallType(mallType)
                .aladinCategoryId(aladinCategoryId)
                .categoryName(categoryName)
                .build();
    }
}
