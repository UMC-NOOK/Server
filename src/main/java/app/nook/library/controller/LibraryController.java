package app.nook.library.controller;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryService;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library")
@Validated
public class LibraryController {

    private final LibraryService libraryService;

    // 서재 책 등록
    @PostMapping("/{bookId}")
    public ApiResponse<Void> registerBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookId
    ) {
        libraryService.save(userDetails.getUser(), bookId);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    // 서재 책 삭제
    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> deleteBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookId
    ) {
        libraryService.deleteById(userDetails.getUser(), bookId);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    // 서재 책 상태 변경
    @PatchMapping("/status")
    public ApiResponse<Void> changeStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReadingStatusRequestDto requestDto
    ) {
        libraryService.changeStatus(userDetails.getUser(), requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    // 서재 상태별 책 조회
    @GetMapping("/status")
    public ApiResponse<LibraryViewDto.StatusBookResponseDto> viewBooksByStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @NotNull ReadingStatus status,
            @RequestParam(required = false) @Min(0) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        LibraryViewDto.StatusBookResponseDto response =
                libraryService.viewBooksByStatus(userDetails.getUser(), status, cursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 책 개수 조회
    @GetMapping("/count")
    public ApiResponse<LibraryViewDto.BookCountResponseDto> viewBookCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LibraryViewDto.BookCountResponseDto response =
                libraryService.countBooks(userDetails.getUser());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 해당 날짜의 포커스 기록 반환
    @GetMapping("/focus-records")
    public ApiResponse<CursorResponse<LibraryViewDto.UserBookResponseDto, Long>> viewFocusRecordByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam
            @NotNull
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate date,
            @RequestParam(required = false) @Min(0) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        CursorResponse<LibraryViewDto.UserBookResponseDto, Long> response =
                libraryService.viewFocusRecordByDate(userDetails.getUser(), date, cursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 읽기 전 상태의 책 5권 조회
    @GetMapping("/before-reading")
    public ApiResponse<LibraryViewDto.BeforeReadingResponseDto> viewBeforeReadingBooks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LibraryViewDto.BeforeReadingResponseDto response =
                libraryService.viewBeforeReadingBooks(userDetails.getUser());
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 가장 최근에 포커스한 책, page 조회
    @GetMapping("/recent-focus")
    public ApiResponse<LibraryViewDto.RecentFocusResponseDto> viewRecentFocus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LibraryViewDto.RecentFocusResponseDto response =
                libraryService.viewRecentFocus(userDetails.getUser());
        if(response == null)
            return ApiResponse.onSuccess(null, SuccessCode.NO_CONTENT);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

}
