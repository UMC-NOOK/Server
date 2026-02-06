package app.nook.aladin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AladinResponseDto {

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AladinApiResult {
        private Long totalResults;
        private Integer startIndex;
        private Integer itemsPerPage;
        private List<AladinItem> item;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    // 알라딘 API 아이템 DTO
    public static class AladinItem {
        private String isbn13;
        private String title;
        private String author;
        private String categoryName;
        private Integer categoryId;
        private String mallType;
        private String publisher;
        private String pubDate;
        private String description;
        private String cover;
        private String link;
        private boolean adult;
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
