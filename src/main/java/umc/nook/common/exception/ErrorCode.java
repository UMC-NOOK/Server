package umc.nook.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-001", "사용자를 찾을 수 없습니다."),
    HAS_ID(HttpStatus.CONFLICT, "ACCOUNT-002", "존재하는 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "ACCOUNT-003", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "ACCOUNT-004", "중복된 닉네임입니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST, "ACCOUNT-005", "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "ACCOUNT-006", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "ACCOUNT-007", "토큰이 만료되었습니다."),
    LOGINDID_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-008", "존재하지 않는 회원 아이디입니다."),
    INVALID_LOGINID(HttpStatus.NOT_FOUND, "ACCOUNT-009", "유효하지 않은 아이디입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "ACCOUNT-011", "유효하지 않은 닉네임입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
