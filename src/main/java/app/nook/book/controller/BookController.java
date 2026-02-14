package app.nook.book.controller;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.service.BookService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
@Validated
public class BookController {
    private final BookService bookService;

    // 도서 상세 조회
    @GetMapping("/{isbn13}")
    public ApiResponse<BookResponseDto.BookDetailDto> getBookDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Pattern(regexp = "\\d{13}", message = "ISBN13은 13자리 숫자여야 합니다.") String isbn13) {
        return ApiResponse.onSuccess(
                bookService.getBookDetailByIsbn(userDetails.getUser(),isbn13), SuccessCode.OK);
    }

    // 주간 베스트셀러 조회
    @GetMapping("/bestsellers")
    public ApiResponse<List<BookResponseDto.BookPreviewDto>> getBestsellers() {
        return ApiResponse.onSuccess(bookService.getWeeklyBestsellers(), SuccessCode.OK);
    }

    // 사용자 맞춤 추천 베스트셀러 조회
    @GetMapping("/recommendations")
    public ApiResponse<List<BookResponseDto.BookPreviewDto>> getPersonalizedBestsellers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                bookService.getPersonalizedBestsellers(userDetails.getUser().getId()), SuccessCode.OK);
    }
}

