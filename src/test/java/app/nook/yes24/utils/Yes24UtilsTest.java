package app.nook.yes24.utils;

import app.nook.yes24.dto.Yes24ResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Yes24UtilsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "국내도서-경제 경영, 경제경영",
            "국내도서-소설/시/희곡, 소설/시/희곡",
            "국내도서-가정 살림, 가정/요리/뷰티",
            "국내도서-인물, 에세이",
            "국내도서-IT 모바일, IT",
            "국내도서-대학교재, 전문서적",
            "국내도서-유아-토이북, 유아",
            "국내도서-만화/라이트노벨-라이트노벨, 만화",
            "국내도서-가정과생활, 가정/요리/뷰티",
            "국내도서-자기관리, 자기계발",
            "국내도서-건강과취미, 건강/취미/레저",
            "국내도서-자연과과학, 과학"
    })
    @DisplayName("V4 매핑: 2단계 분류명(옛 이름 포함)을 서비스 카테고리로 변환")
    void resolveServiceCategory_mapped(String goodsSortNm, String expected) {
        assertThat(Yes24Utils.resolveServiceCategory(goodsSortNm)).contains(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "국내도서-IT 모바일-컴퓨터수험서",
            "국내도서-수험서 자격증",
            "국내도서-전집",
            "국내도서-잡지",
            "국내도서-초등학습서",
            "국내도서-중고등학습서-고등학교",
            "eBook-소설",
            "외국도서-소설",
            "국내도서",
            "국내도서-처음보는분류"
    })
    @DisplayName("국내도서가 아니거나 미노출·미등록 분류는 제외")
    void resolveServiceCategory_excluded(String goodsSortNm) {
        assertThat(Yes24Utils.resolveServiceCategory(goodsSortNm)).isEmpty();
    }

    @Test
    @DisplayName("ISBN13·성인 여부·카테고리를 모두 통과해야 유효")
    void isValid() {
        assertThat(Yes24Utils.isValid(item("9788937427282", "N", "국내도서-소설/시/희곡"))).isTrue();
        assertThat(Yes24Utils.isValid(item("", "N", "국내도서-소설/시/희곡"))).isFalse();
        assertThat(Yes24Utils.isValid(item("978893742728", "N", "국내도서-소설/시/희곡"))).isFalse();
        assertThat(Yes24Utils.isValid(item("9788937427282", "Y", "국내도서-소설/시/희곡"))).isFalse();
        assertThat(Yes24Utils.isValid(item("9788937427282", null, "국내도서-소설/시/희곡"))).isFalse();
        assertThat(Yes24Utils.isValid(item("9788937427282", "N", "국내도서-전집"))).isFalse();
        assertThat(Yes24Utils.isValid(null)).isFalse();
    }

    private Yes24ResponseDto.Yes24Item item(String isbn13, String adultYn, String goodsSortNm) {
        return Yes24ResponseDto.Yes24Item.builder()
                .isbn13(isbn13)
                .adultYn(adultYn)
                .goodsSortNm(goodsSortNm)
                .build();
    }
}
