package app.nook.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ACCOUNT-007", "인증이 필요합니다."),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "ACCOUNT-008", "중복된 이메일입니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "ACCOUNT-009", "권한이 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "ACCOUNT-010", "탈퇴한 사용자입니다."),
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "ACCOUNT-014", "유효하지 않은 소셜 로그인 방법입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
