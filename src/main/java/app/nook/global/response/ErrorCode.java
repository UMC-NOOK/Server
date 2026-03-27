package app.nook.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode {

    // 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-002", "요청 파라미터가 올바르지 않습니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "DATE-001", "유효하지 않은 날짜입니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "FORMAT-001", "형식이 올바르지 않습니다."),
    JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "COMMON-003", "JSON 파싱에 실패했습니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "COMMON-004", "동시 수정 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "ACCOUNT-002", "중복된 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "ACCOUNT-003", "비밀번호가 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "ACCOUNT-004", "유효하지 않는 리프레시 토큰입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "ACCOUNT-005", "유효하지 않는 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCOUNT-006", "토큰이 만료되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ACCOUNT-007", "인증이 필요합니다."),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "ACCOUNT-008", "중복된 이메일입니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "ACCOUNT-009", "권한이 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "ACCOUNT-010", "탈퇴한 사용자입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-011", "리프레시 토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCOUNT-012", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "ACCOUNT-013", "리프레시 토큰이 유효하지 않습니다."),
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "ACCOUNT-014", "유효하지 않은 소셜 로그인 방법입니다."),

    // 서재 관련
    DUPLICATE_BOOK_IN_SHELF(HttpStatus.CONFLICT, "BOOKSHELF-001", "이미 서재에 등록된 책입니다."),
    ALREADY_READING(HttpStatus.CONFLICT, "BOOKSHELF-002", "이미 독서중 상태입니다."),
    ALREADY_FINISHED(HttpStatus.CONFLICT, "BOOKSHELF-003", "완독한 책은 다시 읽기로 변경할 수 없습니다."),
    BOOK_NOT_EXIST(HttpStatus.NOT_FOUND, "BOOKSHELF-004", "서재에 등록되지 않은 책입니다."),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "BOOKSHELF-005", "형식이 잘못되었습니다. yyyy-MM 형식으로 입력해주세요."),
    BOOKSHELF_IS_EMPTY(HttpStatus.NOT_FOUND, "BOOKSHELF-006", "현재 읽고 있는 책이 없습니다."),
    ALREADY_REGISTERED_TODAY(HttpStatus.CONFLICT, "BOOK-007", "해당 날짜에는 이미 책이 등록되었습니다."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "BOOKSHELF-008", "독서 상태를 변경할 수 없습니다."),
    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "BOOKSHELF-009", "이미 찜한 책입니다."),
    RECORDED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "BOOKSHELF-010", "recordedAt값은 필수입니다."),

    // 리딩룸 관련
    READING_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM-001", "리딩룸을 찾을 수 없습니다."),
    ALREADY_JOINED_READING_ROOM(HttpStatus.CONFLICT, "ROOM-002", "이미 가입한 리딩룸입니다."),
    ROOM_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "ROOM-003", "리딩룸의 최대 인원 수를 초과했습니다."),
    HOST_ONLY(HttpStatus.FORBIDDEN, "ROOM-004", "리딩룸의 호스트만 수행할 수 있습니다."),
    USER_NOT_JOINED_ROOM(HttpStatus.NOT_FOUND, "ROOM-005", "리딩룸에 가입하지 않은 사용자입니다."),
    TOO_MANY_JOINED_ROOM(HttpStatus.CONFLICT, "ROOM-006", "리딩룸은 최대 4개까지 가입 가능합니다."),

    // 기록
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD-001", "기록이 존재하지 않습니다."),
    INVALID_RECORD_TYPE(HttpStatus.BAD_REQUEST, "RECORD-002", "유효하지 않은 기록 유형입니다."),
    RECORD_NOT_EXIST(HttpStatus.NOT_FOUND, "RECORD-003", "기록이 없습니다."),
    CHAT_RECORD_MUST_BE_COMMENT(HttpStatus.BAD_REQUEST, "RECORD-004", "감상의 경우에만 저장 가능합니다."),
    CHAT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD-003", "채팅 기록이 존재하지 않습니다."),

    // OAUTH
    INVALID_OAUTH_TOKEN(HttpStatus.BAD_REQUEST, "OAUTH-001", "유효하지 않은 카카오 토큰입니다."),
    INVALID_KAKAO_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "OAUTH-002", "유효하지 않은 카카오 리프레시 토큰입니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "OAUTH-003", "유효하지 않은 카카오ID입니다."),
    KAKAO_UNLINK_FAILED(HttpStatus.BAD_REQUEST, "OAUTH-004", "카카오 계정 연결 해제에 실패하였습니다."),

    // GPT
    GPT_RESPONSE_FORMAT_ERROR(HttpStatus.BAD_GATEWAY, "GPT-001", "GPT 응답 포맷이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
