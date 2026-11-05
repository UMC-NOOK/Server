package app.nook.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 닉네임 규칙: 공백 포함 1~10자, 영문/숫자/한글/공백만 허용, 앞뒤 공백 불가(중간 공백은 허용).
 * 위반 사유별로 메시지가 달라 클라이언트가 모달을 구분할 수 있다.
 */
@Documented
@Constraint(validatedBy = NickNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NickName {

    String EMPTY_MESSAGE = "닉네임을 입력해주세요.";
    String TOO_LONG_MESSAGE = "닉네임은 공백 포함 최대 10자까지 입력할 수 있습니다.";
    String INVALID_CHAR_MESSAGE = "닉네임은 영문, 숫자, 한글, 공백만 사용할 수 있습니다.";

    String message() default INVALID_CHAR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
