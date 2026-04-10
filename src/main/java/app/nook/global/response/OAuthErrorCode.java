package app.nook.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OAuthErrorCode implements BaseCode {

    INVALID_OAUTH_TOKEN(HttpStatus.BAD_REQUEST, "OAUTH-001", "유효하지 않은 소설로그인 토큰입니다."),
    INVALID_KAKAO_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "OAUTH-002", "유효하지 않은 소셜로그인 리프레시 토큰입니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "OAUTH-003", "유효하지 않은 소셜로그인 ID입니다."),
    KAKAO_UNLINK_FAILED(HttpStatus.BAD_REQUEST, "OAUTH-004", "소셜로그인 계정 연결 해제에 실패하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
