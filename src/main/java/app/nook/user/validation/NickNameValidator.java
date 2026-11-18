package app.nook.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NickNameValidator implements ConstraintValidator<NickName, String> {

    static final int MAX_LENGTH = 10;
    // 영문/숫자/한글(완성형 + 자음·모음 단독)로 시작·종료, 중간에만 공백 허용
    private static final String CHAR = "a-zA-Z0-9가-힣ㄱ-ㅣ";
    private static final Pattern ALLOWED =
            Pattern.compile("^[" + CHAR + "](?:[" + CHAR + " ]*[" + CHAR + "])?$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 필수 여부는 @NotNull로 판단 (프로필 수정처럼 선택 필드에서 허용)
        }
        if (value.isBlank()) {
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
