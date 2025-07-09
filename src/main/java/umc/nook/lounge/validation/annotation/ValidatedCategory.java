package umc.nook.lounge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.lounge.validation.validator.CategoryValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = CategoryValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedCategory {
    String message() default "유효하지 않은 'categoryId' 값입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
