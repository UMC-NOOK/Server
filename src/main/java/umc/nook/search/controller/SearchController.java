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
import umc.nook.search.service.SearchService;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "search", description = "검색 API")
@Validated
public class SearchController {

    private final SearchService searchService;

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
}
