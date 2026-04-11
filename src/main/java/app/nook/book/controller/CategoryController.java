package app.nook.book.controller;

import app.nook.global.api.Api1Version;
import app.nook.book.dto.CategoryResponseDto;
import app.nook.book.service.CategoryService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api1Version
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/book")
    public ApiResponse<CategoryResponseDto.BookCategoryList> getBookCategories() {
        return ApiResponse.onSuccess(categoryService.getBookCategories(), SuccessCode.OK);
    }
}
