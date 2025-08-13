package umc.nook.bookshelves.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.dto.SortType;
import umc.nook.bookshelves.service.BookShelfService;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.ErrorCode;
import umc.nook.common.response.SuccessCode;
import umc.nook.users.service.CustomUserDetails;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookshelf")
@Tag(name = "Bookshelf API", description = "서재 API")
public class BookShelfController {

    private final BookShelfService bookshelfService;

    @PostMapping("/register")
    @Operation(summary = "서재 책 등록", description = "서재에 책을 등록합니다.")
    public ApiResponse<String> registerBook (@RequestBody @Validated BookShelfDTO.RegisterBookDTO registerBookDTO,
                                             @AuthenticationPrincipal CustomUserDetails userDetails){
        String result = bookshelfService.registerBook(registerBookDTO, userDetails.getUser());
        return ApiResponse.onSuccess(result, SuccessCode.CREATED);
    }

    @DeleteMapping("/delete/{bookId}")
    @Operation(summary = "서재 책 삭제", description = "서재에서 책을 삭제합니다.")
    public ApiResponse<String> deleteBook (@PathVariable Long bookId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String result = bookshelfService.deleteBook(bookId, userDetails.getUser());
        return ApiResponse.onSuccess(result, SuccessCode.ACCEPTED);
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
    @Parameters({
            @Parameter(name = "status", description = "서재 상태: BOOKMARK / READING / FINISHED", required = true, example = "READING"),
            @Parameter(name = "page", description = "0부터 시작하는 페이지 번호", example = "0"),
            @Parameter(name = "size", description = "페이지 크기 (기본 8)", example = "8"),
            @Parameter(name = "sort", description = "정렬: RECENT(최근 기록순) / LATEST(최근 등록순) / TITLE(제목순) / RATING(내가 준 별점순)", example = "RECENT")
    })
    public ApiResponse<BookShelfDTO.PageDTO<BookShelfDTO.UserBookListResponseDTO>> getUserBooks(
            @RequestParam ReadingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "LATEST") SortType sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var response = bookshelfService.getUserBooks(userDetails.getUser(), status, page, size, sort);
        return ApiResponse.onSuccess(response,SuccessCode.OK);
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

    // 홈 화면 통계 조회
    @GetMapping("/insight")
    @Operation(summary = "홈 화면 독서 통계 조회", description = "사용자의 전체 책 수와 상태별 책 수를 반환합니다.")
    public ApiResponse<BookShelfDTO.BooksInsightDTO> getBooksInsight(@AuthenticationPrincipal CustomUserDetails userDetails) {
        BookShelfDTO.BooksInsightDTO insight = bookshelfService.viewBooksInsight(userDetails.getUser());
        return ApiResponse.onSuccess(insight,SuccessCode.OK);
    }

    // 홈 화면 독서중인 책 조회
    @GetMapping("/reading")
    @Operation(summary = "홈 화면 독서중인 책 조회", description = "사용자가 현재 읽고 있는 책을 조회합니다.")
    public ApiResponse<BookShelfDTO.BookThumbnail> viewReadingBook(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                bookshelfService.viewReadingBooks(userDetails.getUser()),
                SuccessCode.OK
        );
    }

    @GetMapping("/weekly")
    @Operation(summary = "이번 주 등록한 책 목록 조회", description = "사용자가 이번 주에 서재에 등록한 책들을 조회합니다.")
    public ApiResponse<List<BookShelfDTO.WeeklyBooksDTO>> viewWeeklyBookShelf(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                bookshelfService.viewWeeklyBookShelf(userDetails.getUser()),
                SuccessCode.OK
        );
    }


}
