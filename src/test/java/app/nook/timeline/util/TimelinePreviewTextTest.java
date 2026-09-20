package app.nook.timeline.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimelinePreviewText 테스트")
class TimelinePreviewTextTest {

    @Test
    @DisplayName("100자 이하면 그대로 반환한다")
    void 이하는_그대로() {
        String text = "가".repeat(100);

        assertThat(TimelinePreviewText.truncate(text)).isEqualTo(text);
    }

    @Test
    @DisplayName("100자를 넘으면 앞 100자만 남긴다")
    void 초과는_100자로_자른다() {
        String text = "가".repeat(100) + "나".repeat(50);

        assertThat(TimelinePreviewText.truncate(text)).isEqualTo("가".repeat(100));
    }

    @Test
    @DisplayName("이모지 같은 서로게이트 문자도 글자 단위로 세고 중간에서 깨지지 않는다")
    void 이모지는_깨지지_않는다() {
        String text = "😀".repeat(101);

        String result = TimelinePreviewText.truncate(text);

        assertThat(result.codePointCount(0, result.length())).isEqualTo(100);
        assertThat(result).isEqualTo("😀".repeat(100));
    }

    @Test
    @DisplayName("null은 그대로 반환한다")
    void null은_그대로() {
        assertThat(TimelinePreviewText.truncate(null)).isNull();
    }
}
