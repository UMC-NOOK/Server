package app.nook.book.controller;

import app.nook.api.Api1Version;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.facade.BookSearchFacade;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api1Version
@RequestMapping("/books/search")
@RequiredArgsConstructor
@Validated
public class BookSearchController {

    private final BookSearchFacade bookSearchFacade;

    /**
     * 전체 도서 검색 (알라딘 API)
     * - 커서 기반 페이지네이션
     */
    @GetMapping("/{type}")
    public ApiResponse<BookResponseDto.SearchResultDto> searchGlobal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable SearchType type,
            @RequestParam(name = "keyword")
                @NotBlank(message = "검색어를 입력해주세요.")
                @Size(max = 100, message = "검색어는 100자를 초과할 수 없습니다.") String keyword,
            @RequestParam(name = "cursor", required = false)
                @Min(value = 0, message = "커서는 0 이상이어야 합니다.") Integer cursor
    ) {
        BookResponseDto.SearchResultDto response = bookSearchFacade.searchBooks(
                userDetails.getUser().getId(), keyword, cursor, type);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    /**
     * 최근 검색어 조회
     * - global / bookcase 타입별 조회
     */
    @GetMapping("/{type}/histories")
    public ApiResponse<List<String>> getSearchHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable SearchType type
    ) {
        List<String> searchHistories = bookSearchFacade.getSearchHistories(userDetails.getUser().getId(), type);
        return ApiResponse.onSuccess(searchHistories, SuccessCode.OK);
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @DeleteMapping("/{type}/histories")
    public ApiResponse<Void> deleteSearchHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "keyword") @NotBlank(message = "삭제할 검색어를 입력해주세요.") String keyword,
            @PathVariable SearchType type
    ) {
        bookSearchFacade.deleteSearchHistory(userDetails.getUser().getId(), keyword, type);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    /**
     * 최근 검색어 전체 삭제
     */
    @DeleteMapping("/{type}/histories/all")
    public ApiResponse<Void> deleteAllSearchHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable SearchType type
    ) {
        bookSearchFacade.deleteAllSearchHistories(userDetails.getUser().getId(), type);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }
}
