package app.nook.focus.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FocusErrorCode implements BaseCode {

    FOCUS_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "FOCUS-001", "이미 진행 중인 포커스가 있습니다."),
    FOCUS_NOT_FOUND(HttpStatus.NOT_FOUND, "FOCUS-002", "포커스를 찾을 수 없습니다."),
    FOCUS_ALREADY_ENDED(HttpStatus.CONFLICT, "FOCUS-003", "이미 종료된 포커스입니다."),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOCUS-004", "서재의 책을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
