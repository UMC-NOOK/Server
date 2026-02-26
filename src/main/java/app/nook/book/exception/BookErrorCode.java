package app.nook.book.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseCode {

    ISBN13_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "요청한 ISBN-13에 해당하는 도서를 찾을 수 없습니다."),
    INVALID_ISBN13(HttpStatus.BAD_REQUEST, "BOOK-002", "유효하지 않은 ISBN-13 형식입니다."),
    BOOK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "BOOK-003", "서비스 정책에 의해 조회할 수 없는 도서입니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-004", "존재하지 않는 책입니다."),
    BOOK_NOT_OWNED(HttpStatus.FORBIDDEN, "BOOK-005", "본인이 생성한 사용자 도서만 수정할 수 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "BOOK-006", "지원하지 않는 이미지 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK-007", "이미지 업로드에 실패했습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "BOOK-008", "유효하지 않은 카테고리입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
