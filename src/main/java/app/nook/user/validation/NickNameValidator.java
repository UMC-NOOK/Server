package app.nook.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NickNameValidator implements ConstraintValidator<NickName, String> {

    static final int MAX_LENGTH = 10;
    // 영문/숫자/한글로 시작·종료, 중간에만 공백 허용
    private static final Pattern ALLOWED =
            Pattern.compile("^[a-zA-Z0-9가-힣](?:[a-zA-Z0-9가-힣 ]*[a-zA-Z0-9가-힣])?$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return fail(context, NickName.EMPTY_MESSAGE);
        }
        if (value.length() > MAX_LENGTH) {
            return fail(context, NickName.TOO_LONG_MESSAGE);
        }
        if (!ALLOWED.matcher(value).matches()) {
            return fail(context, NickName.INVALID_CHAR_MESSAGE);
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
