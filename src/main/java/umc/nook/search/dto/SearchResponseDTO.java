package umc.nook.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class SearchResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String coverImageUrl;
        private String publicationDate;
        private String mallType;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaginationDTO{
        private int currentPage;
        private int pageSize;
        private long totalItems;
        private int totalPages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchResultDTO{
        private List<BookDTO> books;
        private PaginationDTO pagination;
    }
}