package app.nook.book.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.dto.CategoryResponseDto;
import app.nook.book.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category essay;
    private Category fiction;

    @BeforeEach
    void setUp() {
        essay = Category.of(MallType.BOOK, "에세이", 55889);
        fiction = Category.of(MallType.BOOK, "소설/시/희곡", 1);

        ReflectionTestUtils.setField(essay, "id", 1L);
        ReflectionTestUtils.setField(fiction, "id", 2L);
    }

    @Test
    @DisplayName("도서 카테고리 조회 성공")
    void getBookCategories_success() {
        given(categoryRepository.findAllByMallTypeOrderByCategoryNameAsc(MallType.BOOK))
                .willReturn(List.of(essay, fiction));

        CategoryResponseDto.BookCategoryList result = categoryService.getBookCategories();

        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories().get(0).categoryId()).isEqualTo(1L);
        assertThat(result.categories().get(0).categoryName()).isEqualTo("에세이");
        assertThat(result.categories().get(0).aladinCategoryId()).isEqualTo(55889);
        assertThat(result.categories().get(1).categoryId()).isEqualTo(2L);
        assertThat(result.categories().get(1).categoryName()).isEqualTo("소설/시/희곡");
        assertThat(result.categories().get(1).aladinCategoryId()).isEqualTo(1);

        verify(categoryRepository).findAllByMallTypeOrderByCategoryNameAsc(MallType.BOOK);
    }

    @Test
    @DisplayName("도서 카테고리 조회 결과가 비어있으면 빈 리스트를 반환한다")
    void getBookCategories_empty() {
        given(categoryRepository.findAllByMallTypeOrderByCategoryNameAsc(MallType.BOOK))
                .willReturn(List.of());

        CategoryResponseDto.BookCategoryList result = categoryService.getBookCategories();

        assertThat(result.categories()).isEmpty();

        verify(categoryRepository).findAllByMallTypeOrderByCategoryNameAsc(MallType.BOOK);
    }
}
