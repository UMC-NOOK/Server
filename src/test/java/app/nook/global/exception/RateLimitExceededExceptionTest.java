package app.nook.global.exception;

import app.nook.global.response.CommonErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceededExceptionTest {

    @ParameterizedTest
    @CsvSource({
            "0, 1초 후 다시 시도해 주세요.",
            "30, 30초 후 다시 시도해 주세요.",
            "59, 59초 후 다시 시도해 주세요.",
            "60, 1분 후 다시 시도해 주세요.",
            "61, 2분 후 다시 시도해 주세요.",
            "600, 10분 후 다시 시도해 주세요."
    })
    void 남은_시간은_60초_미만은_초_이상은_분_올림으로_안내한다(long seconds, String description) {
        RateLimitExceededException e = RateLimitExceededException.retryAfter(
                CommonErrorCode.TOO_MANY_REQUESTS, "요청 한도에 도달했습니다.", seconds);

        assertThat(e.getDescription()).isEqualTo(description);
        assertThat(e.getMessage()).isEqualTo("요청 한도에 도달했습니다. " + description);
        assertThat(e.getRetryAfterSeconds()).isEqualTo(Math.max(1, seconds));
    }
}
