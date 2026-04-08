package app.nook.book.controller;

import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.facade.UserBookFacade;
import app.nook.book.service.BookService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
@Validated
public class BookController {
    private final BookService bookService;
    private final UserBookFacade userBookFacade;

    // ISBN 기반 상세조회: ALADIN 도서 조회 진입점
    @GetMapping("/{isbn13}")
    public ApiResponse<BookResponseDto.BookDetailDto> getBookDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Pattern(regexp = "\\d{13}", message = "ISBN13은 13자리 숫자여야 합니다.") String isbn13) {
        return ApiResponse.onSuccess(
                bookService.getBookDetailByIsbn(userDetails.getUser(), isbn13), SuccessCode.OK);
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
                bookService.getPersonalizedBestsellers(userDetails.getUser()), SuccessCode.OK);
    }

    // bookId 기반 상세조회: 서재/내부 목록에서 상세 진입 시 사용
    @GetMapping("/id/{bookId}")
    public ApiResponse<BookResponseDto.BookDetailDto> getBookDetailsById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive(message = "bookId는 1 이상이어야 합니다.") Long bookId
    ) {
        return ApiResponse.onSuccess(
                bookService.getBookDetailById(userDetails.getUser(), bookId), SuccessCode.OK);
    }

    // 사용자 도서 등록
    @PostMapping("/user")
    public ApiResponse<BookResponseDto.BookDetailDto> createUserBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BookRequestDto.CreateUserBookRequest request
    ) {
        return ApiResponse.onSuccess(
                userBookFacade.createUserBook(userDetails.getUser(), request), SuccessCode.CREATED);
    }

    // 사용자 도서 수정 (본인 생성 USER 도서만 허용)
    // coverImageKey 미전송/null이면 기존 표지 유지, 삭제는 별도 API로 처리
    @PatchMapping("/user/{bookId}")
    public ApiResponse<BookResponseDto.BookDetailDto> updateUserBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive(message = "bookId는 1 이상이어야 합니다.") Long bookId,
            @Valid @RequestBody BookRequestDto.UpdateUserBookRequest request
    ) {
        return ApiResponse.onSuccess(
                userBookFacade.updateUserBook(userDetails.getUser(), bookId, request), SuccessCode.OK
        );
    }
}
