package app.nook.redis.exception;

import lombok.Getter;

@Getter
public class RedisOperationException extends RuntimeException {

    private final RedisErrorCode errorCode;

    public RedisOperationException(RedisErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
