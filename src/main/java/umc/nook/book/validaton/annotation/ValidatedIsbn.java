package umc.nook.book.validaton.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.book.validaton.validator.IsbnValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = IsbnValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedIsbn {
    String message() default "유효하지 않은 ISBN-13 형식입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
