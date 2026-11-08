package app.nook.global.exception;

import app.nook.global.response.BaseCode;
import lombok.Getter;

/**
 * 요청 한도 초과(429) 공용 예외
 * - 응답: Retry-After 헤더 + result{title, description}, message는 title + " " + description
 * - 에러 코드를 인자로 받아 COMMON-006 외의 429 응답에도 사용할 수 있다.
 */
@Getter
public class RateLimitExceededException extends CustomException {

    private final String title;
    private final String description;
    private final long retryAfterSeconds;

    public RateLimitExceededException(BaseCode errorCode, String title, String description, long retryAfterSeconds) {
        super(errorCode, title + " " + description);
        this.title = title;
        this.description = description;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /** 남은 시간 안내 문구: 60초 미만은 "N초 후", 60초 이상은 "N분 후"(올림) */
    public static RateLimitExceededException retryAfter(BaseCode errorCode, String title, long retryAfterSeconds) {
        long seconds = Math.max(1, retryAfterSeconds);
        String wait = seconds < 60 ? seconds + "초" : ((seconds + 59) / 60) + "분";
        return new RateLimitExceededException(errorCode, title, wait + " 후 다시 시도해 주세요.", seconds);
    }

    public Result result() {
        return new Result(title, description);
    }

    public record Result(String title, String description) {
    }
}
