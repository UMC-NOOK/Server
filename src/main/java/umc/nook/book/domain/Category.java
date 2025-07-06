package umc.nook.book.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
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
}
