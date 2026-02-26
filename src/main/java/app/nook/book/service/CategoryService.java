package app.nook.book.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.dto.CategoryResponseDto;
import app.nook.book.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResponseDto.BookCategoryList getBookCategories() {
        List<Category> categories = categoryRepository.findAllByMallTypeOrderByCategoryNameAsc(MallType.BOOK);
        List<CategoryResponseDto.BookCategoryItem> items = categories.stream()
                .map(c -> {
                    return new CategoryResponseDto.BookCategoryItem(
                            c.getId(),
                            c.getCategoryName(),
                            c.getAladinCategoryId()
                    );
                }).toList();
        return new CategoryResponseDto.BookCategoryList(items);
    }
}
