package umc.nook.review.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.review.validation.annotation.ValidatedRating;

public class RatingValidator implements ConstraintValidator<ValidatedRating, Integer> {
    @Override
    public void initialize(ValidatedRating constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Integer rating, ConstraintValidatorContext constraintValidatorContext) {
        return rating != null && rating >= 0 && rating <= 5;
    }
}
