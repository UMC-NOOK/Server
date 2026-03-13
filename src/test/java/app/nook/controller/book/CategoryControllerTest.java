package app.nook.controller.book;

import app.nook.book.controller.CategoryController;
import app.nook.book.dto.CategoryResponseDto;
import app.nook.book.service.CategoryService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class CategoryControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithCustomUser
    @DisplayName("도서 카테고리 조회 성공")
    void 도서카테고리_조회_성공() throws Exception {
        CategoryResponseDto.BookCategoryList response = new CategoryResponseDto.BookCategoryList(
                List.of(
                        new CategoryResponseDto.BookCategoryItem(1L, "에세이", 55889),
                        new CategoryResponseDto.BookCategoryItem(2L, "소설/시/희곡", 1)
                )
        );

        given(categoryService.getBookCategories()).willReturn(response);

        mockMvc.perform(get("/api/categories/book")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories[0].categoryId").value(1L))
                .andExpect(jsonPath("$.result.categories[0].categoryName").value("에세이"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.categories[].categoryId").description("카테고리 ID"),
                                        fieldWithPath("result.categories[].categoryName").description("카테고리명"),
                                        fieldWithPath("result.categories[].aladinCategoryId").description("알라딘 카테고리 ID")
                                )
                        )
                ));
    }

    @Test
    @DisplayName("도서 카테고리 조회 - 인증 없음 401")
    void 도서카테고리_조회_인증없음_401() throws Exception {
        mockMvc.perform(get("/api/categories/book"))
                .andExpect(status().isUnauthorized());
    }
}
