package umc.nook.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "ACCOUNT-002", "중복된 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "ACCOUNT-003", "비밀번호가 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST,"ACCOUNT-004" ,"유효하지 않는 리프레시 토큰입니다." ),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST,"ACCOUNT-005" ,"유효하지 않는 토큰입니다." ),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCOUNT-006", "토큰이 만료되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ACCOUNT-007", "인증이 필요합니다."),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST,"ACCOUNT-008" ,"중복된 이메일입니다." ),

    // 페이지
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "PAGE-001", "유효하지 않은 'page' 값입니다."),
    INVALID_LIMIT(HttpStatus.BAD_REQUEST, "PAGE-002", "유효하지 않은 'limit' 값입니다."),

    // 라운지
    INVALID_MALLTYPE(HttpStatus.BAD_REQUEST, "LOUNGE-001", "유효하지 않은 'mallType' 값입니다."),
    INVALID_SECTION(HttpStatus.BAD_REQUEST, "LOUNGE-002", "유효하지 않은 'sectionId' 값입니다."),
    INVALID_CATEGORY(HttpStatus.NOT_FOUND, "LOUNGE-003", "요청한 카테고리가 존재하지 않습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
