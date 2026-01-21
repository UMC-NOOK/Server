package app.nook.aladin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AladinResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookDetailDto { // 검색 도서 상세 DTO
        private String isbn13;
        private String title;
        private String author;
        private String categoryName;
        private Integer categoryId;
        private String publisher;
        private String pubDate;
        private String description;
        private String coverImageUrl;
        private String aladinLink;
        private Integer itemPage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookPreviewDto { // 검색 베스트셀러 프리뷰 DTO
        private String isbn13;
        private String title;
        private String author;
        private String coverImageUrl;
        private String publisher;
        private Integer rank;
    }


    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AladinApiResult {
        private List<AladinItem> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    // 알라딘 API 아이템 DTO
    public static class AladinItem {
        private String isbn13;
        private String title;
        private String author;
        private String categoryName;
        private Integer categoryId;
        private String publisher;
        private String pubDate;
        private String description;
        private String cover;
        private String link;
        private Integer bestRank;
        private SubInfo subInfo;

        public Integer getItemPage() {
            return subInfo != null ? subInfo.getItemPage() : null;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubInfo {
        private Integer itemPage;
    }
}
