package umc.nook.review.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.review.validation.validator.RatingValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = RatingValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedRating {
    String message() default "유효하지 않은 평점입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
