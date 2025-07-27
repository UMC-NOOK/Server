package umc.nook.lounge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.lounge.dto.LoungeResponseDTO;
import umc.nook.lounge.service.LoungeService;
import umc.nook.lounge.validation.annotation.*;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Lounge", description = "라운지 도서 API")
@Validated
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
                    description = "조회할 도서의 몰 타입 (RECOMMENDATION, BOOK, FOREIGN, EBOOK / 기본값: RECOMMENDATION)",
                    required = false,
                    example = "RECOMMENDATION"
            ),
            @Parameter(
                    name = "sectionId",
                    description = "각 섹션의 ID (best, new, favorite_best)",
                    required = false
            ),
            @Parameter(
                    name = "categoryId",
                    description = "페이지네이션을 적용할 카테고리의 알라딘 ID. 응답 본문에서 categoryID 값 참고",
                    required = false
            ),
            @Parameter(
                    name = "page",
                    description = "조회할 페이지 번호 (기본값: 1)",
                    required = false,
                    example = "1"
            )
    })
    @GetMapping("lounge/books")
    public ApiResponse<LoungeResponseDTO.LoungeBookResultDTO> getLoungeBooks(
            @ValidatedMallType @RequestParam(defaultValue = "RECOMMENDATION") String mallType,
            @ValidatedSection @RequestParam(required = false) String sectionId,
            @ValidatedCategory @RequestParam(required = false) Integer categoryId,
            @ValidatedPage @RequestParam(defaultValue = "1") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        LoungeResponseDTO.LoungeBookResultDTO result = loungeService.getLoungeBooks(mallType, sectionId, categoryId, page, userDetails);
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }


    @Operation(
            summary = "홈 화면 선호 카테고리",
            description = """
            홈 화면에서 가장 많이 읽은 카테고리들을 조회합니다.
            top 5 + 나머지는 기타로 합쳐서 처리합니다.
            """
    )
    @GetMapping("/categories")
    public ApiResponse<LoungeResponseDTO.CategoryResultDTO> getFavoriteCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(loungeService.getFavoriteCategories(userDetails), SuccessCode.OK);
    }

    @Operation(
            summary = "홈 화면 목표 조회",
            description = """
            홈 화면에서 설정한 독서 목표를 조회합니다.
            
            """
    )
    @GetMapping("/goals")
    public ApiResponse<LoungeResponseDTO.GoalResultDTO> getGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(loungeService.getGoal(userDetails), SuccessCode.OK);
    }

    @Operation(
            summary = "홈 화면 목표 설정",
            description = """
            홈 화면에서 독서 목표를 설정합니다.
            목표는 50, 100, 150, 200, 250, 300 중 하나의 값입니다.
            """
    )
    @PatchMapping("/goals")
    public ApiResponse<Void> modifyGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody LoungeResponseDTO.GoalRequestDTO goalRequestDTO
            ) {
        loungeService.modifyGoal(userDetails, goalRequestDTO);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }
}
