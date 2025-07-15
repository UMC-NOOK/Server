package umc.nook.lounge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.validation.annotation.ValidatedPage;

public class PageValidator implements ConstraintValidator<ValidatedPage, Integer> {
    @Override
    public void initialize(ValidatedPage constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Integer page, ConstraintValidatorContext constraintValidatorContext) {
        if (page <= 0) {
            throw new CustomException(ErrorCode.INVALID_PAGE);
        }
        return true;
    }
}
