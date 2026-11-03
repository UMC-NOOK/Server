package app.nook.global.ratelimit;

import lombok.Getter;

import java.time.Duration;

/**
 * 앱 요청 제한 정책 (고정 윈도우). 출시 후 한도 조정은 이곳에서만 한다.
 * - IMAGE_DAY는 window 대신 다음 KST 자정까지 유지된다.
 */
@Getter
public enum RateLimitPolicy {

    LOGIN(30, Duration.ofMinutes(1)),
    RECOVER(10, Duration.ofMinutes(10)),
    REISSUE(60, Duration.ofMinutes(1)),
    SEARCH_NEW(30, Duration.ofMinutes(1)),
    SEARCH_SCROLL(60, Duration.ofMinutes(1)),
    BOOK_DETAIL(60, Duration.ofMinutes(1)),
    IMAGE_MINUTE(20, Duration.ofMinutes(1)),
    IMAGE_DAY(100, null);

    private final long limit;
    private final Duration window;

    RateLimitPolicy(long limit, Duration window) {
        this.limit = limit;
        this.window = window;
    }

    /** Redis 키 구분자 (예: IMAGE_MINUTE → image-minute) */
    public String keyName() {
        return name().toLowerCase().replace('_', '-');
    }
}
