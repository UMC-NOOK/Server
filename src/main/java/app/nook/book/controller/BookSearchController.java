package app.nook.book.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.ratelimit.RateLimitPolicy;
import app.nook.global.ratelimit.RateLimiter;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.dto.LibrarySearchHomeResponseDto;
import app.nook.book.facade.BookSearchFacade;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
    private final RateLimiter rateLimiter;

    /**
     * 전체 도서 검색 (외부 도서 API)
     * - 커서 기반 페이지네이션
     */
    @GetMapping("/{type}")
    public ApiResponse<BookResponseDto.SearchResultDto> searchGlobal(
            @CurrentUser User user,
            @PathVariable SearchType type,
            @RequestParam(name = "keyword")
                @NotBlank(message = "검색어를 입력해주세요.")
                @Size(max = 100, message = "검색어는 100자를 초과할 수 없습니다.") String keyword,
            @RequestParam(name = "cursor", required = false)
                @Min(value = 0, message = "커서는 0 이상이어야 합니다.") Integer cursor
    ) {
        // 외부 도서 API를 호출하는 GLOBAL만 제한. 첫 페이지와 다음 페이지는 별도 집계
        if (type == SearchType.GLOBAL) {
            RateLimitPolicy policy = (cursor == null || cursor == 0)
                    ? RateLimitPolicy.SEARCH_NEW
                    : RateLimitPolicy.SEARCH_SCROLL;
            rateLimiter.consume(policy, String.valueOf(user.getId()));
        }
        BookResponseDto.SearchResultDto response = bookSearchFacade.searchBooks(
                user.getId(), keyword, cursor, type);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/library/home")
    public ApiResponse<LibrarySearchHomeResponseDto.Result> getLibrarySearchHome(
            @CurrentUser User user
    ) {
        LibrarySearchHomeResponseDto.Result response = bookSearchFacade.getLibrarySearchHome(user);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    /**
     * 최근 검색어 조회
     * - global / bookcase 타입별 조회
     */
    @GetMapping("/{type}/histories")
    public ApiResponse<List<String>> getSearchHistories(
            @CurrentUser User user,
            @PathVariable SearchType type
    ) {
        List<String> searchHistories = bookSearchFacade.getSearchHistories(user.getId(), type);
        return ApiResponse.onSuccess(searchHistories, SuccessCode.OK);
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @DeleteMapping("/{type}/histories")
    public ApiResponse<Void> deleteSearchHistory(
            @CurrentUser User user,
            @RequestParam(name = "keyword") @NotBlank(message = "삭제할 검색어를 입력해주세요.") String keyword,
            @PathVariable SearchType type
    ) {
        bookSearchFacade.deleteSearchHistory(user.getId(), keyword, type);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    /**
     * 최근 검색어 전체 삭제
     */
    @DeleteMapping("/{type}/histories/all")
    public ApiResponse<Void> deleteAllSearchHistories(
            @CurrentUser User user,
            @PathVariable SearchType type
    ) {
        bookSearchFacade.deleteAllSearchHistories(user.getId(), type);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }
}
