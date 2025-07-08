package umc.nook.lounge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.lounge.dto.LoungeResponseDTO;
import umc.nook.lounge.service.LoungeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lounge")
@Tag(name = "Lounge", description = "라운지 도서 API")
public class LoungeController {

    private final LoungeService loungeService;

    @Operation(
            summary = "라운지 도서 목록 조회",
            description = """
            라운지 페이지에서 ‘추천’, ‘국내 도서’, ‘외국 도서’, ‘eBook’ 몰 타입 별로 도서 목록을 조회합니다.
            ‘추천’은 주간 베스트셀러와 개인화 추천, 나머지는 신간 및 분야별 베스트셀러를 제공합니다.
            """
    )
    @Parameters({
            @Parameter(
                    name = "mallType",
                    description = "조회할 도서의 몰 타입 (RECOMMENDATION, BOOK, FOREIGN, EBOOK)",
                    required = true,
                    example = "BOOK"
            ),
            @Parameter(
                    name = "sectionId",
                    description = "각 섹션의 ID (best, new, favorite_best)",
                    required = false,
                    example = "best"
            ),
            @Parameter(
                    name = "categoryId",
                    description = "페이지네이션을 적용할 카테고리의 알라딘 ID. 응답 본문에서 categoryID 값 참고",
                    required = false,
                    example = "1"
            ),
            @Parameter(
                    name = "queryType",
                    description = "베스트셀러/신간 구분 (BESTSELLER, ITEMNEWALL)",
                    required = false,
                    example = "BESTSELLER"
            ),
            @Parameter(
                    name = "page",
                    description = "조회할 페이지 번호 (기본값: 1)",
                    required = false,
                    example = "1"
            ),
            @Parameter(
                    name = "limit",
                    description = "한 페이지에 보여줄 도서 개수 (기본값: 6)",
                    required = false,
                    example = "6"
            )
    })
    @GetMapping("/books")
    public ApiResponse<LoungeResponseDTO.LoungeBookResultDTO> getLoungeBooks(
            @RequestParam String mallType,
            @RequestParam(required = false) String sectionId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String queryType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int limit,
            @Parameter(hidden = true) @RequestHeader("Authorization") String token

    ) {
        LoungeResponseDTO.LoungeBookResultDTO result = loungeService.getLoungeBooks(
                mallType, sectionId, categoryId, queryType, page, limit, token
        );
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }
}
