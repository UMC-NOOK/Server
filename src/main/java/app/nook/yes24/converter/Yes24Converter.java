package app.nook.yes24.converter;

import app.nook.book.domain.enums.MallType;
import app.nook.book.dto.BookResponseDto;
import app.nook.yes24.dto.Yes24ResponseDto;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

public class Yes24Converter {

    private static final Pattern COMPACT_DATE = Pattern.compile("\\d{8}");
    private static final Pattern LINE_BREAK_TAG = Pattern.compile("(?i)<br\\s*/?>|</p>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    public static BookResponseDto.BookPreviewDto toBookPreviewDto(Yes24ResponseDto.Yes24Item item, int rank) {
        return new BookResponseDto.BookPreviewDto(
                item.getIsbn13(),
                decode(item.getTitle()),
                decode(item.getAuthor()),
                item.getCover(),
                decode(item.getPublisher()),
                rank
        );
    }

    public static BookResponseDto.BookSearchDto toBookSearchDto(Yes24ResponseDto.Yes24Item item) {
        return BookResponseDto.BookSearchDto.builder()
                .isbn13(item.getIsbn13())
                .title(decode(item.getTitle()))
                .mallType(MallType.BOOK.getDisplayName())
                .author(decode(item.getAuthor()))
                .coverImageUrl(item.getCover())
                .publisher(decode(item.getPublisher()))
                .publicationDate(normalizeDate(item.getPublishDate()))
                .build();
    }

    public static BookResponseDto.BookDetailDto toBookDetailDto(Yes24ResponseDto.Yes24Item item, String categoryName) {
        return BookResponseDto.BookDetailDto.builder()
                .isbn13(item.getIsbn13())
                .title(decode(item.getTitle()))
                .author(decode(item.getAuthor()))
                .mallType(MallType.BOOK.getDisplayName())
                .mallTypeCode(MallType.BOOK)
                .category(categoryName)
                .publisher(decode(item.getPublisher()))
                .publicationDate(normalizeDate(item.getPublishDate()))
                .pages(item.getPages())
                .description(toPlainText(item.getBookIntroduction()))
                .coverImageUrl(item.getCover())
                .purchaseLink(item.getLink())
                .build();
    }

    // "20221118" -> "2022-11-18". 이미 "2022-11-18" 형식이면 그대로
    static String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (COMPACT_DATE.matcher(trimmed).matches()) {
            return trimmed.substring(0, 4) + "-" + trimmed.substring(4, 6) + "-" + trimmed.substring(6);
        }
        return trimmed;
    }

    // 소개글의 <b>, <br>, <p>, <iframe> 등을 제거하고 줄바꿈만 남긴다
    static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        // 줄바꿈은 \r\n(CRLF)으로 와서 \n으로 통일
        String withBreaks = LINE_BREAK_TAG.matcher(html.replaceAll("\\r\\n?", "\n")).replaceAll("\n");
        String text = HtmlUtils.htmlUnescape(HTML_TAG.matcher(withBreaks).replaceAll(""));
        String normalized = text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String decode(String value) {
        return value == null ? null : HtmlUtils.htmlUnescape(value);
    }
}
