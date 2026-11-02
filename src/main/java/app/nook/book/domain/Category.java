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
@Table(
        indexes = {
                @Index(name = "idx_category_malltype_name",
                        columnList = "mall_type, category_name")
        }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="category_id")
    private Long id;

    @Column(name = "category_name", length = 50)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name="mall_type")
    private MallType mallType;

    // 맞춤 추천 베스트셀러 조회용 YES24 카테고리 코드 (예: 001001046)
    @Column(name = "yes24_category_id", length = 12, nullable = false)
    private String yes24CategoryId;

    @OneToMany(mappedBy = "category")
    private List<Book> books = new ArrayList<>();

    @Builder
    public Category(String categoryName, MallType mallType, String yes24CategoryId) {
        this.categoryName = categoryName;
        this.mallType = mallType;
        this.yes24CategoryId = yes24CategoryId;
    }

    public static Category of(MallType mallType, String categoryName, String yes24CategoryId) {
        return Category.builder()
                .mallType(mallType)
                .yes24CategoryId(yes24CategoryId)
                .categoryName(categoryName)
                .build();
    }
}
