package app.nook.aladin.converter;

import app.nook.aladin.dto.AladinResponseDto;
import org.springframework.stereotype.Component;


public class AladinConverter {
    public static AladinResponseDto.BookPreviewDto toBookPreviewDto(AladinResponseDto.AladinItem aladinItem) {
        return AladinResponseDto.BookPreviewDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(aladinItem.getTitle())
                .author(aladinItem.getAuthor())
                .coverImageUrl(aladinItem.getCover())
                .publisher(aladinItem.getPublisher())
                .rank(aladinItem.getBestRank())
                .build();
    }
}
