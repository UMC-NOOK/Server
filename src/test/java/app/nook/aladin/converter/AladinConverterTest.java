package app.nook.aladin.converter;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.dto.BookResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AladinConverterTest {

    @Test
    @DisplayName("도서 DTO 변환 - HTML 엔티티는 한 번만 변환하고 표지와 카테고리는 유지")
    void toBookDtos_HTML엔티티단회변환() {
        // given
        AladinResponseDto.AladinItem item = AladinResponseDto.AladinItem.builder()
                .isbn13("9780000000000")
                .title("&lt;Title&gt; &amp; &quot;Quote&quot; &#65; &#x42; &amp;lt; &unknown;")
                .author("A &amp; B")
                .publisher("&lt;출판사&gt;")
                .description("설명 &amp; 소개")
                .cover("https://img.test/a&amp;b.jpg")
                .link("https://aladin.test/item?ItemId=1&amp;partner=2&amp;start=3")
                .categoryName("국내도서&gt;소설/시/희곡")
                .mallType("BOOK")
                .build();

        // when
        BookResponseDto.BookPreviewDto preview = AladinConverter.toBookPreviewDto(item, 1);
        BookResponseDto.BookSearchDto search = AladinConverter.toBookSearchDto(item);
        BookResponseDto.BookDetailDto detail = AladinConverter.toBookDetailDto(item, "소설/시/희곡");

        // then
        assertThat(preview.title()).isEqualTo("<Title> & \"Quote\" A B &lt; &unknown;");
        assertThat(preview.author()).isEqualTo("A & B");
        assertThat(preview.publisher()).isEqualTo("<출판사>");
        assertThat(search.getTitle()).isEqualTo(preview.title());
        assertThat(search.getAuthor()).isEqualTo(preview.author());
        assertThat(search.getPublisher()).isEqualTo(preview.publisher());
        assertThat(detail.getTitle()).isEqualTo(preview.title());
        assertThat(detail.getAuthor()).isEqualTo(preview.author());
        assertThat(detail.getPublisher()).isEqualTo(preview.publisher());
        assertThat(detail.getDescription()).isEqualTo("설명 & 소개");
        assertThat(detail.getAladinLink()).isEqualTo("https://aladin.test/item?ItemId=1&partner=2&start=3");
        assertThat(detail.getCoverImageUrl()).isEqualTo("https://img.test/a&amp;b.jpg");
        assertThat(detail.getCategory()).isEqualTo("소설/시/희곡");
    }

    @Test
    @DisplayName("도서 상세 DTO 변환 - null과 일반 문자열 유지")
    void toBookDetailDto_null과일반문자열유지() {
        // given
        AladinResponseDto.AladinItem item = AladinResponseDto.AladinItem.builder()
                .title("<꽉TV>")
                .author(null)
                .publisher("출판사")
                .description(null)
                .link(null)
                .mallType("BOOK")
                .build();

        // when
        BookResponseDto.BookDetailDto detail = AladinConverter.toBookDetailDto(item, "소설/시/희곡");

        // then
        assertThat(detail.getTitle()).isEqualTo("<꽉TV>");
        assertThat(detail.getAuthor()).isNull();
        assertThat(detail.getPublisher()).isEqualTo("출판사");
        assertThat(detail.getDescription()).isNull();
        assertThat(detail.getAladinLink()).isNull();
    }
}
