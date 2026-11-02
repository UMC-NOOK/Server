package app.nook.yes24.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class Yes24ResponseDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Yes24ApiResult {
        private boolean success;
        private String message;
        private String errorCode;
        private Data data;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<Yes24Item> items;
        private Long totalCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Yes24Item {
        private String title;
        private String author;
        private String goodsSortNm;   // "국내도서-소설/시/희곡" 형태의 관리 분류
        private String adultYn;
        private String publisher;
        private String isbn13;
        private String publishDate;   // yyyyMMdd 또는 yyyy-MM-dd
        private String cover;
        private String link;
        private Integer pages;        // detail=Y에서만 옴
        private ContentDetail contentDetail;

        public String getBookIntroduction() {
            return contentDetail != null ? contentDetail.getBookIntroduction() : null;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentDetail {
        private String bookIntroduction;
    }

    // 검색 원본 페이지 (검색 캐시 단위)
    public record SearchPage(List<Yes24Item> items, long totalCount) {
        public static SearchPage empty() {
            return new SearchPage(List.of(), 0L);
        }
    }
}
