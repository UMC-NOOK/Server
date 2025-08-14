package umc.nook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.review.validation.annotation.ValidatedRating;
import umc.nook.review.validation.annotation.ValidatedReview;

public class ReviewRequestDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @ValidatedReview
    public static class ReviewCreateDTO{

        @NotNull(message = "평점은 필수 입력값입니다.")
        @Min(value = 0, message = "평점은 0 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5 이하이어야 합니다.")
        private Integer rating;

        private String content;
    }
}
