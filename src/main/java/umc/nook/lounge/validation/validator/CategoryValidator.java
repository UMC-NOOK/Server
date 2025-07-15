package umc.nook.lounge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import umc.nook.book.service.CategoryService;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.validation.annotation.ValidatedCategory;

@RequiredArgsConstructor
public class CategoryValidator implements ConstraintValidator<ValidatedCategory, Integer> {

    private final CategoryService categoryService;

    @Override
    public void initialize(ValidatedCategory constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Integer categoryId, ConstraintValidatorContext constraintValidatorContext) {
        if (categoryId == null) {
            return true;
        }

        if (!categoryService.isCategoryValid(categoryId)) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
        return true;
    }
}
