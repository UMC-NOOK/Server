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
        private String categoryName;
        private String pubDate;
        private String mallType;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDetailDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String pubDate;
        private String mallType;
        private int categoryId;
        private String categoryName;
        private String description;
        private String cover;
        private SubInfo subInfo;

        public Integer getItemPage() {
            return subInfo != null ? subInfo.getItemPage() : null;
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BestInThisCategoryDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String cover;
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

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LookUpResultDTO{
        private List<BookDetailDTO> item;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubInfo{
        private Integer itemPage;
    }
}
