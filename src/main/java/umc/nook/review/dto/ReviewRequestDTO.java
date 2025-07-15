package umc.nook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

        @ValidatedRating
        private int rating;

        private String content;
    }
}
