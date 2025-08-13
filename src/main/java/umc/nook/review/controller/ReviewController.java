package umc.nook.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.nook.book.validaton.annotation.ValidatedIsbn;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.lounge.validation.annotation.ValidatedPage;
import umc.nook.review.dto.ReviewRequestDTO;
import umc.nook.review.dto.ReviewResponseDTO;
import umc.nook.review.service.ReviewService;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
@Tag(name = "review", description = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "리뷰 목록 조회",
            description = """
            특정 도서에 등록된 모든 리뷰를 페이지 단위로 조회합니다.
            리뷰는 최신순으로 정렬되어 있습니다.
            """
    )
    @Parameters({
            @Parameter(
                    name = "bookId",
                    description = "조회할 책의 식별자.",
                    required = true
            ),
            @Parameter(
                    name = "page",
                    description = "조회할 페이지 번호 (기본값: 0)",
                    required = false,
                    example = "0"
            )
    })
    @GetMapping("/books/{bookId}/reviews")
    public ApiResponse<ReviewResponseDTO.ReviewResultDTO> getReviews(
            @PathVariable Long bookId,
            @ValidatedPage @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(reviewService.getReviews(bookId, userDetails, page), SuccessCode.OK);
    }

    @Operation(
            summary = "리뷰 작성",
            description = """
            특정 도서에 대한 리뷰를 작성하고 등록합니다.
            동일한 도서에 중복 리뷰를 작성할 수 없습니다.
            평점, 리뷰 내용은 선택 사항 (둘중 하나는 있어야 함)
            """
    )
    @Parameters({
            @Parameter(
                    name = "bookId",
                    description = "리뷰를 작성할 책의 식별자.",
                    required = true
            )
    })
    @PostMapping("/books/{bookId}/reviews")
    public ApiResponse<ReviewResponseDTO.ReviewDTO> addReview(
            @PathVariable Long bookId,
            @RequestBody @Validated ReviewRequestDTO.ReviewCreateDTO review,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ApiResponse.onSuccess(reviewService.addReview(bookId, review, userDetails), SuccessCode.CREATED);
    }

    @Operation(
            summary = "리뷰 수정",
            description = """
            자신이 작성한 리뷰의 평점이나 내용을 수정합니다.
            """
    )
    @Parameters({
            @Parameter(
                    name = "reviewId",
                    description = "수정할 리뷰의 고유 식별자",
                    required = true
            )
    })
    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponseDTO.ReviewDTO> modifyReview(
            @PathVariable Long reviewId,
            @RequestBody @Validated ReviewRequestDTO.ReviewCreateDTO reviewDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ApiResponse.onSuccess(
                reviewService.modifyReview(reviewId, reviewDTO, userDetails), SuccessCode.OK
        );
    }

    @Operation(
            summary = "리뷰 삭제",
            description = """
            자신이 작성한 리뷰를 삭제합니다.
            """
    )
    @Parameters({
            @Parameter(
                    name = "reviewId",
                    description = "삭제할 리뷰의 고유 식별자.",
                    required = true
            )
    })
    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reviewService.deleteReview(reviewId, userDetails);
        return ApiResponse.onSuccess(null, SuccessCode.ACCEPTED);
    }


}
