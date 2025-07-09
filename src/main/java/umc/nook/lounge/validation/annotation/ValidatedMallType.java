package umc.nook.lounge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.lounge.validation.validator.MallTypeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MallTypeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedMallType {
    String message() default "유효하지 않은 'mallType' 값입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
