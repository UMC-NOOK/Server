package app.nook.aladin.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AladinErrorCode implements BaseCode {

    ALADIN_API_ERROR(HttpStatus.BAD_GATEWAY, "ALADIN-001", "알라딘 API 호출 중 오류가 발생했습니다."),
    ALADIN_PARSING_ERROR(HttpStatus.BAD_GATEWAY, "ALADIN-002", "알라딘 API 응답 형식이 올바르지 않습니다."),
    ALADIN_UNAUTHORIZED(HttpStatus.INTERNAL_SERVER_ERROR, "ALADIN-003", "알라딘 API 인증에 실패했습니다."),
    ALADIN_INVALID_MALLTYPE(HttpStatus.BAD_GATEWAY, "ALADIN-004", "유효하지 않은 MallType 입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
