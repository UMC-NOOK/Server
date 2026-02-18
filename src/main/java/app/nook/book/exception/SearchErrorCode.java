package app.nook.book.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SearchErrorCode implements BaseCode {

    INVALID_KEYWORD(HttpStatus.BAD_REQUEST, "SEARCH-001", "필수 파라미터 'keyword' 가 누락되었습니다."),
    SEARCH_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH-002", "최근 검색어를 찾을 수 없습니다."),
    INVALID_SEARCH_TYPE(HttpStatus.BAD_REQUEST, "SEARCH-003", "유효하지 않은 검색 유형입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
