package umc.nook.aladin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WrapperDTO {
        private String version;
        private String logo;
        private String title;
        private String link;
        private String pubDate;
        private int totalResults;
        private int startIndex;
        private int itemsPerPage;
        private List<AladinResponseDTO.BookDetailDTO> item;
    }


    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaginationDTO{
        private int totalResults;
        private int startIndex;
        private int itemsPerPage;
    }

    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultDTO extends PaginationDTO{
        private List<BookDetailDTO> item;
    }

    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResultDTO extends PaginationDTO{
        private List<SearchBookDTO> item;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LookUpResultDTO{
        private List<BookDetailDTO> item;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubInfo{
        private Integer itemPage;
    }
}
