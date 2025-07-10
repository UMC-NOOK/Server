package umc.nook.lounge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import umc.nook.lounge.validation.validator.SectionValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = SectionValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedSection {
    String message() default "유효하지 않은 'sectionId' 값입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
