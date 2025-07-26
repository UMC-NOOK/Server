package umc.nook.aladin.converter;

import umc.nook.aladin.dto.AladinResponseDTO;

import java.util.stream.Collectors;

public class AladinConverter {

    public static AladinResponseDTO.LoungeBookDTO toLoungeBookDTO(AladinResponseDTO.BookDetailDTO bookDetailDTO) {
        return AladinResponseDTO.LoungeBookDTO.builder()
                .isbn13(bookDetailDTO.getIsbn13())
                .title(bookDetailDTO.getTitle())
                .author(bookDetailDTO.getAuthor())
                .categoryName(bookDetailDTO.getCategoryName())
                .publisher(bookDetailDTO.getPublisher())
                .cover(bookDetailDTO.getCover())
                .build();
    }

    public static AladinResponseDTO.SearchBookDTO toSearchBookDTO(AladinResponseDTO.BookDetailDTO bookDetailDTO) {
        return AladinResponseDTO.SearchBookDTO.builder()
                .isbn13(bookDetailDTO.getIsbn13())
                .title(bookDetailDTO.getTitle())
                .author(bookDetailDTO.getAuthor())
                .publisher(bookDetailDTO.getPublisher())
                .cover(bookDetailDTO.getCover())
                .categoryName(bookDetailDTO.getCategoryName())
                .pubDate(bookDetailDTO.getPubDate())
                .mallType(bookDetailDTO.getMallType())
                .build();
    }
}
