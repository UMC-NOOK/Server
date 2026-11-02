package app.nook.yes24.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Yes24ConverterTest {

    @Test
    @DisplayName("출간일: yyyyMMdd는 yyyy-MM-dd로, 이미 yyyy-MM-dd면 그대로")
    void normalizeDate() {
        assertThat(Yes24Converter.normalizeDate("20221118")).isEqualTo("2022-11-18");
        assertThat(Yes24Converter.normalizeDate("2013-12-24")).isEqualTo("2013-12-24");
        assertThat(Yes24Converter.normalizeDate(" ")).isNull();
        assertThat(Yes24Converter.normalizeDate(null)).isNull();
    }

    @Test
    @DisplayName("소개글: 태그 제거, <br>·</p>는 줄바꿈, HTML 엔티티 복원")
    void toPlainText() {
        assertThat(Yes24Converter.toPlainText("<p><b>첫</b> 문단</p><p>둘째 &amp; 문단</p>"))
                .isEqualTo("첫 문단\n둘째 & 문단");
        assertThat(Yes24Converter.toPlainText("가<br>나<BR/>다")).isEqualTo("가\n나\n다");
        assertThat(Yes24Converter.toPlainText("<iframe src=\"https://www.youtube.com/embed/x\"></iframe>")).isNull();
        assertThat(Yes24Converter.toPlainText("첫 줄 \r\n둘째 줄\r\n\r\n셋째 문단")).isEqualTo("첫 줄\n둘째 줄\n\n셋째 문단");
        assertThat(Yes24Converter.toPlainText(null)).isNull();
    }
}
