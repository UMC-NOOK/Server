package umc.nook.review.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.review.validation.validator.ReviewValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Constraint(validatedBy = ReviewValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedReview {
    String message() default "유효하지 않은 리뷰입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
