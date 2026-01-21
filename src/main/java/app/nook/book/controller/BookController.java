package app.nook.book.controller;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.service.BookService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    // 주간 베스트셀러 조회
    @GetMapping("/bestsellers")
    public ApiResponse<List<AladinResponseDto.BookPreviewDto>> getBestsellers() {
        return ApiResponse.onSuccess(bookService.getWeeklyBestsellers(), SuccessCode.OK);
    }

    // 사용자 맞춤 추천 베스트셀러 조회
    @GetMapping("/recommendations")
    public ApiResponse<List<AladinResponseDto.BookPreviewDto>> getPersonalizedBestsellers() {
        return ApiResponse.onSuccess(bookService.getPersonalizedBestsellers(), SuccessCode.OK);
    }
}

