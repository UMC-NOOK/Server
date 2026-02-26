package app.nook.book.controller;

import app.nook.book.dto.CategoryResponseDto;
import app.nook.book.service.CategoryService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/book")
    public ApiResponse<CategoryResponseDto.BookCategoryList> getBookCategories() {
        return ApiResponse.onSuccess(categoryService.getBookCategories(), SuccessCode.OK);
    }
}
