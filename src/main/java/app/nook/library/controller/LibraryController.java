package app.nook.library.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryCommandService;
import app.nook.library.service.LibraryQueryService;
import app.nook.library.util.LibraryBookCursorCodec;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/library")
@Validated
public class LibraryController {

    private final LibraryCommandService libraryCommandService;
    private final LibraryQueryService libraryQueryService;

    // 서재 책 등록
    @PostMapping("/{bookId}")
    public ApiResponse<LibraryViewDto.BookStatusResponseDto> registerBook(
            @CurrentUser User user,
            @PathVariable Long bookId
    ) {
        LibraryViewDto.BookStatusResponseDto response =
                libraryCommandService.registerBook(user.getId(), bookId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 책 삭제
    @DeleteMapping("/{bookId}")
    public ApiResponse<LibraryViewDto.BookStatusResponseDto> deleteBook(
            @CurrentUser User user,
            @PathVariable Long bookId
    ) {
        LibraryViewDto.BookStatusResponseDto response =
                libraryCommandService.deleteByBookId(user.getId(), bookId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 책 상태 변경
    @PatchMapping("/status")
    public ApiResponse<LibraryViewDto.BookStatusResponseDto> changeStatus(
            @CurrentUser User user,
            @Valid @RequestBody ReadingStatusRequestDto requestDto
    ) {
        LibraryViewDto.BookStatusResponseDto response =
                libraryCommandService.changeReadingStatus(user.getId(), requestDto);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 상태별 책 조회
    @GetMapping("/status")
    public ApiResponse<LibraryViewDto.StatusBookResponseDto> viewBooksByStatus(
            @CurrentUser User user,
            @RequestParam @NotNull ReadingStatus status,
            @RequestParam(required = false) @Min(0) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long normalizedCursor = (cursor != null && cursor == 0L) ? null : cursor;
        LibraryViewDto.StatusBookResponseDto response =
                libraryQueryService.getBooksByStatus(user.getId(), status, normalizedCursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 책 개수 조회
    @GetMapping("/count")
    public ApiResponse<LibraryViewDto.BookCountResponseDto> viewBookCount(
            @CurrentUser User user
    ) {
        LibraryViewDto.BookCountResponseDto response =
                libraryQueryService.getBookCount(user.getId());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 해당 날짜의 포커스 기록 반환
    @GetMapping("/focus-records")
    public ApiResponse<CursorResponse<LibraryViewDto.UserBookResponseDto, Long>> viewFocusRecordByDate(
            @CurrentUser User user,
            @RequestParam
            @NotNull
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate date,
            @RequestParam(required = false) @Min(1) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        CursorResponse<LibraryViewDto.UserBookResponseDto, Long> response =
                libraryQueryService.getFocusRecordsByDate(user.getId(), date, cursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 읽기 전 상태의 책 5권 조회
    @GetMapping("/before-reading")
    public ApiResponse<LibraryViewDto.BeforeReadingResponseDto> viewBeforeReadingBooks(
            @CurrentUser User user
    ) {
        LibraryViewDto.BeforeReadingResponseDto response =
                libraryQueryService.getBeforeReadingBooks(user.getId());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 가장 최근에 포커스한 책, page, 포커스 시간, 제목 조회
    @GetMapping("/recent-focus")
    public ApiResponse<LibraryViewDto.RecentFocusResponseDto> viewRecentFocus(
            @CurrentUser User user
    ) {
        LibraryViewDto.RecentFocusResponseDto response =
                libraryQueryService.getRecentFocus(user.getId());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 전체 책 목록 상태별 조회 (무한스크롤) - 진입점: 독서 기록
    @GetMapping("/books")
    public ApiResponse<CursorResponse<LibraryViewDto.LibraryBookItem, String>> viewAllBooks(
            @CurrentUser User user,
            @RequestParam(defaultValue = "RECENT_FOCUSED") LibrarySortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        LibraryBookCursor decoded = LibraryBookCursorCodec.decode(cursor);
        CursorResponse<LibraryViewDto.LibraryBookItem, String> response =
                libraryQueryService.getAllBooks(user.getId(), decoded, sort, size);

        if (response.items().isEmpty()) {
            return ApiResponse.onSuccess(null, SuccessCode.NO_CONTENT);
        }

        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 독서 연도 표시 API
    // 사용자 최초 사용 시점부터 현재까지 연도 반환
    @GetMapping("/years")
    public ApiResponse<LibraryViewDto.YearResponseDto> viewReadingYears(
            @CurrentUser User user
    ) {
        LibraryViewDto.YearResponseDto response = libraryQueryService.getReadingYears(user.getId());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }
}
