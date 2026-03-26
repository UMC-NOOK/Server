package app.nook.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum  FileErrorCode implements BaseCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-404", "파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-500", "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-501", "파일 삭제에 실패했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE-400", "유효하지 않은 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-401", "파일 크기가 허용된 최대 크기를 초과했습니다."),
    FILE_NUM_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-401", "파일 개수가 허용된 최대 개수를 초과했습니다."),
    FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FILE-403","파일 접근 권한이 없습니다." );


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
