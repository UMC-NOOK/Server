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


    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND,"BOOK-001" ,"존재하지 않는 책입니다." ),
    DUPLICATE_BOOK_IN_SHELF(HttpStatus.CONFLICT, "BOOKSHELF-001", "이미 서재에 등록된 책입니다."),
    ALREADY_READING(HttpStatus.CONFLICT, "BOOKSHELF-002", "이미 독서중 상태입니다."),
    ALREADY_FINISHED(HttpStatus.CONFLICT, "BOOKSHELF-003", "완독한 책은 다시 읽기로 변경할 수 없습니다."),
    BOOK_NOT_EXIST(HttpStatus.NOT_FOUND,"BOOKSHELF-004" ,"서재에 등록되지 않은 책입니다." ),
    INVALID_MONTH(HttpStatus.BAD_REQUEST,"BOOKSHELF-005", "형식이 잘못되었습니다. yyyy-MM 형식으로 입력해주세요." );


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
