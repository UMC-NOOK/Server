package umc.nook.search.converter;

import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.search.dto.SearchResponseDTO;

public class SearchConverter {

    public static SearchResponseDTO.BookDTO toBookDTO(AladinResponseDTO.SearchBookDTO bookDTO) {
        return SearchResponseDTO.BookDTO.builder()
                .isbn13(bookDTO.getIsbn13())
                .title(bookDTO.getTitle())
                .author(bookDTO.getAuthor())
                .publicationDate(bookDTO.getPubDate())
                .coverImageUrl(bookDTO.getCover())
                .mallType(bookDTO.getMallType())
                .build();
    }

    public static SearchResponseDTO.PaginationDTO toPaginiationDTO(int page, int limit, int totalItems, int totalPages) {
        return SearchResponseDTO.PaginationDTO.builder()
                .currentPage(page)
                .pageSize(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .build();
    }
}
