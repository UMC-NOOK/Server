package app.nook.yes24.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 클라이언트에 공급자 이름이 드러나지 않도록 코드는 BOOK_API로 둔다
@Getter
@AllArgsConstructor
public enum Yes24ErrorCode implements BaseCode {

    BOOK_API_ERROR(HttpStatus.BAD_GATEWAY, "BOOK_API-001", "외부 도서 API 호출 중 오류가 발생했습니다."),
    BOOK_API_UNAUTHORIZED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK_API-002", "외부 도서 API 인증에 실패했습니다."),
    BOOK_API_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "BOOK_API-003", "도서 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
