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

    //리딩룸 관련
    READING_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM-001", "리딩룸을 찾을 수 없습니다."),
    ALREADY_JOINED_READING_ROOM(HttpStatus.CONFLICT, "ROOM-002", "이미 가입한 리딩룸입니다."),
    ROOM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "ROOM-003", "리딩룸의 최대 인원 수를 초과했습니다."),
    HOST_ONLY(HttpStatus.UNAUTHORIZED, "ROOM-004" ,"리딩룸의 호스트만 수행할 수 있습니다." ),

    //테마 관련
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "THEME-001", "테마를 찾을 수 없습니다."),

    //해시태그 관련
    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "HASHTAG-001", "해시태그를 찾을 수 없습니다."),
    TOO_MANY_HASHTAGS(HttpStatus.BAD_REQUEST, "HASHTAG-002", "해시태그는 최대 3개까지 선택 가능합니다."),

    // 페이지
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "PAGE-001", "유효하지 않은 'page' 값입니다."),
    INVALID_LIMIT(HttpStatus.BAD_REQUEST, "PAGE-002", "유효하지 않은 'limit' 값입니다."),

    // 검색
    INVALID_QUERY(HttpStatus.BAD_REQUEST, "SEARCH-001", "필수 파라미터 'query' 가 누락되었습니다."),
    RECENT_QUERY_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH-002", "최근 검색어를 찾을 수 없습니다."),

    // 라운지
    INVALID_MALLTYPE(HttpStatus.BAD_REQUEST, "LOUNGE-001", "유효하지 않은 'mallType' 값입니다."),
    INVALID_SECTION(HttpStatus.BAD_REQUEST, "LOUNGE-002", "유효하지 않은 'sectionId' 값입니다."),
    INVALID_CATEGORY(HttpStatus.NOT_FOUND, "LOUNGE-003", "요청한 카테고리가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
