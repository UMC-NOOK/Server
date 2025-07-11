package umc.nook.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.ErrorCode;
import umc.nook.common.response.SuccessCode;
import umc.nook.lounge.validation.annotation.ValidatedPage;
import umc.nook.search.dto.SearchResponseDTO;
import umc.nook.search.service.RecentQueryService;
import umc.nook.search.service.SearchService;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "search", description = "검색 API")
@Validated
public class SearchController {

    private final SearchService searchService;
    private final RecentQueryService recentQueryService;

    @Operation(
            summary = "책 검색",
            description = """
                    사용자가 입력한 검색어(책 제목, 저자, ISBN)를 기반으로 도서를 검색합니다.
                    """
    )
    @Parameters({
            @Parameter(
                    name = "query",
                    description = "검색할 키워드(책 제목, 저자, ISBN)",
                    required = true
            ),
            @Parameter(
                    name = "page",
                    description = "조회할 페이지 번호 (기본값: 1)",
                    required = false,
                    example = "1"
            )
    })
    @GetMapping("/books")
    public Mono<ApiResponse<SearchResponseDTO.SearchResultDTO>> searchBooks(
            @RequestParam(required = false) String query,
            @ValidatedPage @RequestParam(defaultValue = "1") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (query == null || query.isBlank()) {
            return Mono.error(new CustomException(ErrorCode.INVALID_QUERY));
        }
        return searchService.searchBooks(query, page, userDetails)
                .map(result -> ApiResponse.onSuccess(result, SuccessCode.OK));
    }

    @Operation(
            summary = "최근 검색어 조회",
            description = """
                    사용자의 최근 검색어를 조회합니다.(최대 10개)
                    """
    )
    @GetMapping("/recentQueries")
    public ApiResponse<SearchResponseDTO.RecentQueryResultDTO> getRecentQueries(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(recentQueryService.getRecentQueries(userDetails), SuccessCode.OK);
    }

    @Operation(
            summary = "최근 검색어 삭제",
            description = """
                    특정 최근 검색어를 삭제합니다.
                    """
    )
    @Parameters({
            @Parameter(
                    name = "recentQueryId",
                    description = "삭제할 검색어의 id",
                    required = true
            )
    })
    @DeleteMapping("/recentQueries/{recentQueryId}")
    public ApiResponse<Void> deleteRecentQuery(@PathVariable Long recentQueryId,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        recentQueryService.deleteRecentQuery(userDetails, recentQueryId);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @Operation(
            summary = "최근 검색어 전체 삭제",
            description = """
                    모든 최근 검색어를 삭제합니다.
                    """
    )
    @DeleteMapping("/recentQueries")
    public ApiResponse<Void> deleteAllRecentQueries(@AuthenticationPrincipal CustomUserDetails userDetails) {
        recentQueryService.deleteAllRecentQueries(userDetails);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }
}
