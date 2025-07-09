package umc.nook.lounge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.validation.annotation.ValidatedMallType;

public class MallTypeValidator implements ConstraintValidator<ValidatedMallType, String> {

    @Override
    public void initialize(ValidatedMallType constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String mallType, ConstraintValidatorContext context) {
        if (!mallType.equalsIgnoreCase("RECOMMENDATION")
                && !mallType.equalsIgnoreCase("BOOK")
                && !mallType.equalsIgnoreCase("FOREIGN")
                && !mallType.equalsIgnoreCase("EBOOK")) {
            throw new CustomException(ErrorCode.INVALID_MALLTYPE);
        }
        return true;
    }
}
