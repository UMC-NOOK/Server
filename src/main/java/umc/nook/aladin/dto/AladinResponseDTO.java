package umc.nook.aladin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

public class AladinResponseDTO {

    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaginationDTO{
        private int totalResults;
        private int startIndex;
        private int itemsPerPage;
    }

    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoungeBookDTO{
        private String isbn13;
        private String title;
        private String author;
        private String categoryName;
        private String publisher;
        private String cover;
    }
    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchBookDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String cover;
        private String pubDate;
        private String mallType;
    }
    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoungeResultDTO extends PaginationDTO{
        private List<LoungeBookDTO> item;
    }

    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchResultDTO extends PaginationDTO{
        private List<SearchBookDTO> item;
    }
}
