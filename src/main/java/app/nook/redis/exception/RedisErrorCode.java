package app.nook.redis.exception;

import app.nook.global.response.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RedisErrorCode implements BaseCode {
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "REDIS-001", "Redis 연결에 실패했습니다."),
    REDIS_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "REDIS-002", "Redis 요청 시간이 초과되었습니다."),
    REDIS_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS-003", "Redis 직렬화 처리에 실패했습니다."),
    REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS-004", "Redis 작업 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
