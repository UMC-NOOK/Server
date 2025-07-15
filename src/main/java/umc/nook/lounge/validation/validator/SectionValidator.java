package umc.nook.lounge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.validation.annotation.ValidatedSection;

public class SectionValidator implements ConstraintValidator<ValidatedSection, String> {
    @Override
    public void initialize(ValidatedSection constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String sectionId, ConstraintValidatorContext constraintValidatorContext) {
        if (sectionId == null) {
            return true;
        }

        if (!sectionId.equalsIgnoreCase("best")
                && !sectionId.equalsIgnoreCase("new")
                && !sectionId.equalsIgnoreCase("favorite_best")) {
            throw new CustomException(ErrorCode.INVALID_SECTION);
        }
        return true;
    }
}
