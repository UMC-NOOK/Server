package umc.nook.search.converter;

import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.search.domain.RecentQuery;
import umc.nook.search.dto.SearchResponseDTO;

import java.util.List;

public class SearchConverter {

    public static SearchResponseDTO.BookDTO toBookDTO(AladinResponseDTO.BookDetailDTO bookDTO, Long bookId) {
        return SearchResponseDTO.BookDTO.builder()
                .bookId(bookId)
                .isbn13(bookDTO.getIsbn13())
                .title(bookDTO.getTitle())
                .author(bookDTO.getAuthor())
                .publisher(bookDTO.getPublisher())
                .publicationDate(bookDTO.getPubDate())
                .coverImageUrl(bookDTO.getCover())
                .mallType(bookDTO.getMallType())
                .build();
    }

    public static SearchResponseDTO.PaginationDTO toPaginationDTO(int page, int limit, int totalItems, int totalPages) {
        return SearchResponseDTO.PaginationDTO.builder()
                .currentPage(page)
                .pageSize(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .build();
    }

    public static SearchResponseDTO.RecentQueryResultDTO toRecentQueryResultDTO(List<SearchResponseDTO.RecentQueryDTO> recentQueries) {
        return SearchResponseDTO.RecentQueryResultDTO.builder()
                .recentQueries(recentQueries)
                .build();
    }

    public static SearchResponseDTO.RecentQueryDTO toRecentQueryDTO(RecentQuery recentQuery) {
        return SearchResponseDTO.RecentQueryDTO.builder()
                .recentQueryId(recentQuery.getRecentQueryId())
                .query(recentQuery.getQuery())
                .build();
    }
}
