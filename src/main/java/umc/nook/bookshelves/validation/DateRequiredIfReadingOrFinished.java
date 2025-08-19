package umc.nook.bookshelves.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRequiredValidator.class)
@Documented
public @interface DateRequiredIfReadingOrFinished {
    String message() default "READING 또는 FINISHED 상태에서는 date가 필수입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}