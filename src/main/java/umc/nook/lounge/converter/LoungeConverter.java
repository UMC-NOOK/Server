package umc.nook.lounge.converter;

import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.lounge.dto.LoungeResponseDTO;

import java.util.List;

public class LoungeConverter {

    public static LoungeResponseDTO.BookDTO toBookDTO(AladinResponseDTO.LoungeBookDTO item) {
        return LoungeResponseDTO.BookDTO.builder()
                .isbn13(item.getIsbn13())
                .title(item.getTitle())
                .author(item.getAuthor())
                .publisher(item.getPublisher())
                .coverImageUrl(item.getCover())
                .build();
    }

    public static LoungeResponseDTO.PaginationDTO toPaginiationDTO(int page, int limit, int totalItems, int totalPages) {
        return LoungeResponseDTO.PaginationDTO.builder()
                .currentPage(page)
                .pageSize(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .build();
    }

    public static LoungeResponseDTO.SectionDTO toSectionDTO(String sectionId, Integer categoryId, String categoryName,
                                                            List<LoungeResponseDTO.BookDTO> books,
                                                            LoungeResponseDTO.PaginationDTO pagination) {
        return LoungeResponseDTO.SectionDTO.builder()
                .sectionId(sectionId)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .books(books)
                .pagination(pagination)
                .build();
    }

    public static LoungeResponseDTO.LoungeBookResultDTO toResultDTO(List<LoungeResponseDTO.SectionDTO> sections) {
        return LoungeResponseDTO.LoungeBookResultDTO.builder()
                .sections(sections)
                .build();
    }

}
