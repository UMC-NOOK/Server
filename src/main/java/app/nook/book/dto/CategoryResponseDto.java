package app.nook.book.dto;

import java.util.List;

public class CategoryResponseDto {
    public record BookCategoryItem(
            Long categoryId,
            String categoryName,
            int aladinCategoryId
    ) {}

    public record BookCategoryList(
            List<BookCategoryItem> categories
    ) {}
}
