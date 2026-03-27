package app.nook.record.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum RecordErrorCode implements BaseCode {

    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD-404", "기록을 찾을 수 없습니다."),
    RECORD_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "RECORD-403", "기록 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
