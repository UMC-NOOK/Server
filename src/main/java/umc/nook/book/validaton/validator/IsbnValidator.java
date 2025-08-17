package umc.nook.book.validaton.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.book.validaton.annotation.ValidatedIsbn;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;

public class IsbnValidator implements ConstraintValidator<ValidatedIsbn, String> {
    @Override
    public void initialize(ValidatedIsbn constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null) return false;
        return isbn.chars().allMatch(Character::isDigit) && isbn.length() == 13;
    }

}
