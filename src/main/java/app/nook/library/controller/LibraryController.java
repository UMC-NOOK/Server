package app.nook.library.controller;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryService;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        LibraryViewDto.StatusBookResponseDto response =
                libraryService.viewBooksByStatus(userDetails.getUser(), status, cursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }
}
