package app.nook.aladin.utils;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.domain.enums.MallType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AladinUtilsTest {

    private static final String TEST_ISBN = "9788936434267";
    private static final String TEST_TITLE = "채식주의자";
    private static final String TEST_AUTHOR = "한강";
    private static final String TEST_PUBLISHER = "창비";

    @Test
    @DisplayName("문자열 정규화 - 앞뒤 공백 제거")
    void normalize_공백제거() {
        // given
        String input = "  테스트  ";

        // when
        String result = AladinUtils.normalize(input);

        // then
        assertThat(result).isEqualTo("테스트");
    }

    @Test
    @DisplayName("문자열 정규화 - null 입력 시 null 반환")
    void normalize_null처리() {
        // when
        String result = AladinUtils.normalize(null);

        // then
        assertThat(result).isNull();
    }


    @Test
    @DisplayName("카테고리명 추출 - 정상 케이스")
    void extractCategoryName_정상() {
        // given
        String rawCategory = "국내도서>소설/시/희곡";

        // when
        String result = AladinUtils.extractCategoryName(rawCategory);

        // then
        assertThat(result).isEqualTo("소설/시/희곡");
    }

    @Test
    @DisplayName("카테고리명 추출 - 하나만 있는 경우 빈 문자열")
    void extractCategoryName_깊이1_빈문자열() {
        // given
        String rawCategory = "국내도서";

        // when
        String result = AladinUtils.extractCategoryName(rawCategory);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카테고리명 추출 - null 입력 시 빈 문자열")
    void extractCategoryName_null처리() {
        // when
        String result = AladinUtils.extractCategoryName(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효성 검증 - 정상 케이스 (국내도서, 비성인, 허용 카테고리)")
    void isValid_정상() {
        // given
        AladinResponseDto.AladinItem item = createValidItem("국내도서>소설/시/희곡", false);

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유효성 검증 실패 - null 아이템")
    void isValid_null_실패() {
        // when
        boolean result = AladinUtils.isValid(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 실패 - 19금 도서")
    void isValid_19금_실패() {
        // given
        AladinResponseDto.AladinItem item = createValidItem("국내도서>소설/시/희곡", true); // adult=true

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 실패 - 허용되지 않는 MallType (외국도서)")
    void isValid_허용안된몰타입_실패() {
        // given
        AladinResponseDto.AladinItem item = createItemWithMallType("FOREIGN", "국내도서>소설/시/희곡", false);

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 실패 - 허용되지 않는 카테고리")
    void isValid_허용안된카테고리_실패() {
        // given
        AladinResponseDto.AladinItem item = createValidItem("국내도서>중고샵", false); // 허용되지 않는 카테고리

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 실패 - mallType null")
    void isValid_몰타입null_실패() {
        // given
        AladinResponseDto.AladinItem item = createItemWithMallType(null, "국내도서>소설/시/희곡", false);

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 성공 - 전자책")
    void isValid_전자책_성공() {
        // given
        AladinResponseDto.AladinItem item = createItemWithMallType("EBOOK", "전자책>소설/시/희곡", false);

        // when
        boolean result = AladinUtils.isValid(item);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유효성 검증 성공 - 다양한 허용 카테고리")
    void isValid_다양한카테고리_성공() {
        // 소설/시/희곡
        assertThat(AladinUtils.isValid(createValidItem("국내도서>소설/시/희곡", false))).isTrue();

        // 경제경영
        assertThat(AladinUtils.isValid(createValidItem("국내도서>경제경영", false))).isTrue();

        // 자기계발
        assertThat(AladinUtils.isValid(createValidItem("국내도서>자기계발", false))).isTrue();

        // 에세이
        assertThat(AladinUtils.isValid(createValidItem("국내도서>에세이", false))).isTrue();
    }

    // === 헬퍼 메서드 ===

    /**
     * 정상적인 알라딘 아이템 생성
     */
    private AladinResponseDto.AladinItem createValidItem(String categoryName, boolean isAdult) {
        return createItemWithMallType("BOOK", categoryName, isAdult);
    }

    /**
     * MallType을 지정한 알라딘 아이템 생성
     */
    private AladinResponseDto.AladinItem createItemWithMallType(String mallType, String categoryName, boolean isAdult) {
        return AladinResponseDto.AladinItem.builder()
                .isbn13(TEST_ISBN)
                .title(TEST_TITLE)
                .author(TEST_AUTHOR)
                .publisher(TEST_PUBLISHER)
                .categoryName(categoryName)
                .mallType(mallType)
                .adult(isAdult)
                .cover("http://example.com/cover.jpg")
                .link("http://aladin.com/book")
                .pubDate("2024-01-01")
                .build();
    }
}