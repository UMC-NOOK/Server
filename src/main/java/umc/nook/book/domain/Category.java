package umc.nook.book.domain;

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
    private Long categoryId;

    private String categoryName;

    @Enumerated(value = EnumType.STRING)
    private MallType mallType;

    private int aladinCategoryId;

    @OneToMany(mappedBy = "category")
    private List<Book> books = new ArrayList<>();

    public static Category of(MallType mallType, String categoryName, int aladinCategoryId) {
        return new Category(null, categoryName, mallType, aladinCategoryId, new ArrayList<>());
    }
}
