package umc.nook.book.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.repository.CategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public boolean isCategoryValid(Integer categoryId) {
        return categoryRepository.existsByAladinCategoryId(categoryId);
    }
}
