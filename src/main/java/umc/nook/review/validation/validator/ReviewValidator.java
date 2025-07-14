package umc.nook.review.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.review.dto.ReviewRequestDTO;
import umc.nook.review.validation.annotation.ValidatedReview;

public class ReviewValidator implements ConstraintValidator<ValidatedReview, ReviewRequestDTO.ReviewCreateDTO> {

    @Override
    public void initialize(ValidatedReview constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(ReviewRequestDTO.ReviewCreateDTO dto, ConstraintValidatorContext constraintValidatorContext) {
        if (dto.getRating() == 0) {
            if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_REVIEW);
            }
        }
        return true;
    }
}
