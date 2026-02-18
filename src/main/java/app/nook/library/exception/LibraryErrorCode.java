package app.nook.library.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LibraryErrorCode implements BaseCode {

    BOOK_ALREADY_EXIST(HttpStatus.CONFLICT, "BOOK-409", "서재에 이미 등록된 책입니다."),
    BOOK_STATUS_INVALID(HttpStatus.CONFLICT, "BOOK-409", "이미 반영된 독서 상태입니다."),
    BOOK_NOT_EXIST(HttpStatus.NOT_FOUND, "BOOK-404", "서재에 존재하지 않는 책입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
