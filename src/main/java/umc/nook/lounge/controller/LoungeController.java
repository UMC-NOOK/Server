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
            )
    })
    @GetMapping("lounge/books")
    public ApiResponse<LoungeResponseDTO.LoungeBookResultDTO> getLoungeBooks(
            @ValidatedMallType @RequestParam(defaultValue = "RECOMMENDATION") String mallType,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        LoungeResponseDTO.LoungeBookResultDTO result = loungeService.getLoungeBooks(mallType, userDetails);
        return ApiResponse.onSuccess(result, SuccessCode.OK);
    }
}
