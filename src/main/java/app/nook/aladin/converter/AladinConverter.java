package app.nook.aladin.converter;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.utils.AladinUtils;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.domain.enums.MallType;


public class AladinConverter {
    public static BookResponseDto.BookPreviewDto toBookPreviewDto(AladinResponseDto.AladinItem aladinItem, int rank) {
        return BookResponseDto.BookPreviewDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(aladinItem.getTitle())
                .author(aladinItem.getAuthor())
                .coverImageUrl(aladinItem.getCover())
                .publisher(aladinItem.getPublisher())
                .rank(rank)
                .build();
    }
    public static BookResponseDto.BookDetailDto toBookDetailDto(AladinResponseDto.AladinItem aladinItem, String categoryName) {
        MallType mallType = AladinUtils.extractMallType(aladinItem.getCategoryName());
        return BookResponseDto.BookDetailDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(aladinItem.getTitle())
                .author(aladinItem.getAuthor())
                .mallType(mallType.getDisplayName())
                .mallTypeCode(mallType)
                .category(categoryName)
                .publisher(aladinItem.getPublisher())
                .publicationDate(aladinItem.getPubDate())
                .pages(aladinItem.getItemPage())
                .description(aladinItem.getDescription())
                .coverImageUrl(aladinItem.getCover())
                .aladinLink(aladinItem.getLink())
                .build();
    }

    public static BookResponseDto.BookSearchDTO toBookSearchDto(AladinResponseDto.AladinItem item) {
        return BookResponseDto.BookSearchDTO.builder()
                .isbn13(item.getIsbn13())
                .title(item.getTitle())
                .mallType(AladinUtils.extractMallType(item.getCategoryName()).getDisplayName())
                .author(item.getAuthor())
                .coverImageUrl(item.getCover())
                .publisher(item.getPublisher())
                .publicationDate(item.getPubDate())
                .build();
    }
}
