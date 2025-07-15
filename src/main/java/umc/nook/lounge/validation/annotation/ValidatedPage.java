package umc.nook.lounge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.lounge.validation.validator.PageValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = PageValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedPage {
    String message() default "유효하지 않은 'page' 값입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
