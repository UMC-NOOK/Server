package app.nook.aladin.converter;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.utils.AladinUtils;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.domain.enums.MallType;
import org.springframework.web.util.HtmlUtils;


public class AladinConverter {
    public static BookResponseDto.BookPreviewDto toBookPreviewDto(AladinResponseDto.AladinItem aladinItem, int rank) {
        return new BookResponseDto.BookPreviewDto(
                aladinItem.getIsbn13(),
                decode(aladinItem.getTitle()),
                decode(aladinItem.getAuthor()),
                aladinItem.getCover(),
                decode(aladinItem.getPublisher()),
                rank
        );
    }
    public static BookResponseDto.BookDetailDto toBookDetailDto(AladinResponseDto.AladinItem aladinItem, String categoryName) {
        MallType mallType = AladinUtils.extractMallType(aladinItem.getMallType());
        return BookResponseDto.BookDetailDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(decode(aladinItem.getTitle()))
                .author(decode(aladinItem.getAuthor()))
                .mallType(mallType.getDisplayName())
                .mallTypeCode(mallType)
                .category(categoryName)
                .publisher(decode(aladinItem.getPublisher()))
                .publicationDate(aladinItem.getPubDate())
                .pages(aladinItem.getItemPage())
                .description(decode(aladinItem.getDescription()))
                .coverImageUrl(aladinItem.getCover())
                .aladinLink(decode(aladinItem.getLink()))
                .build();
    }

    public static BookResponseDto.BookSearchDto toBookSearchDto(AladinResponseDto.AladinItem item) {
        return BookResponseDto.BookSearchDto.builder()
                .isbn13(item.getIsbn13())
                .title(decode(item.getTitle()))
                .mallType(AladinUtils.extractMallType(item.getMallType()).getDisplayName())
                .author(decode(item.getAuthor()))
                .coverImageUrl(item.getCover())
                .publisher(decode(item.getPublisher()))
                .publicationDate(item.getPubDate())
                .build();
    }

    private static String decode(String value) {
        return value == null ? null : HtmlUtils.htmlUnescape(value);
    }
}
