package umc.nook.bookshelves.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.service.BookShelfService;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.ErrorCode;
import umc.nook.common.response.SuccessCode;
import umc.nook.users.service.CustomUserDetails;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookshelf")
@Tag(name = "Bookshelf API", description = "서재 API")
public class BookShelfController {

    private final BookShelfService bookshelfService;

    @PostMapping("/register")
    @Operation(summary = "서재 책 등록", description = "서재에 책을 등록합니다.")
    public ApiResponse<String> registerBook (@RequestBody BookShelfDTO.RegisterBookDTO registerBookDTO,
                                             @AuthenticationPrincipal CustomUserDetails userDetails){
        String result = bookshelfService.registerBook(registerBookDTO, userDetails.getUser());
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }

    @DeleteMapping("/delete/{bookId}")
    @Operation(summary = "서재 책 삭제", description = "서재에서 책을 삭제합니다.")
    public ApiResponse<String> deleteBook (@PathVariable Long bookId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String result = bookshelfService.deleteBook(bookId, userDetails.getUser());
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }

    @PatchMapping("/start-reading/{bookId}")
    @Operation(summary = "독서 시작", description = "독서 상태를 시작으로 변경합니다.")
    public ApiResponse<String> changeBookState(@PathVariable Long bookId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        String result = bookshelfService.changeBookState(bookId, userDetails.getUser());
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }

    @GetMapping("/my-books/monthly")
    @Operation(
            summary = "월별 서재 조회",
            description = "입력한 연월(yyyy-MM)에 해당하는 독서 기록을 날짜별로 조회합니다."
    )
    @Parameter(
            name = "yearMonth",
            description = "조회할 연월 (형식: yyyy-MM)",
            example = "2025-06",
            required = true
    )
    public ApiResponse<?> getMonthlyBooks(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            YearMonth parsed = YearMonth.parse(yearMonth);
            var response = bookshelfService.getMonthlyBooks(userDetails.getUser(), parsed);
            return ApiResponse.onSuccess(response, SuccessCode.OK);
        } catch (DateTimeParseException e) {
            return ApiResponse.onFailure(ErrorCode.INVALID_MONTH, null);
        }
    }

    @GetMapping
    @Operation(
            summary = "서재 상태별 조회",
            description = "찜(WISH), 독서중(READING), 완독(COMPLETED) 상태 중 하나를 선택하고, 정렬 기준도 선택하여 조회합니다."
    )
    @Parameters({
            @Parameter(name = "status", description = "서재 상태 필터: BOOKMARK / READING / COMPLETED", required = true, example = "READING"),
            @Parameter(name = "cursorBookId", description = "커서 기반 페이징을 위한 마지막 Book ID", required = false, example = "15"),
            @Parameter(name = "size", description = "가져올 데이터 개수 (기본값: 10)", required = false, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준: recent(최근 등록순), latest(최근 기록순), title(제목순), rating(별점순)", required = false, example = "rating")
    })
    public ApiResponse<?> getUserBooks(
            @RequestParam String status,
            @RequestParam(required = false) Long cursorBookId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recent") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var response = bookshelfService.getUserBooks(userDetails.getUser(), status, cursorBookId, size, sort);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/registered-dates")
    @Operation(summary = "해당 월의 책 등록 날짜 목록 조회")
    @Parameter(
            name = "yearMonth",
            description = "조회할 연월 (형식: yyyy-MM)",
            example = "2025-06",
            required = true
    )
    public ApiResponse<BookShelfDTO.RegisteredBookListResponseDTO> getRegisteredDatesInMonth(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        return ApiResponse.onSuccess(
                bookshelfService.viewRegisteredDatesInMonth(userDetails.getUser(), yearMonth),SuccessCode.OK);
    }

    // TODO 최근 남긴 독서 기록 - 책명, 책id, 기록 id
    // TODO 지금 독서 중인 책 - 책명, 책id, 기록id
    // TODO 서재 독서 통계

}
