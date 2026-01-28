package app.nook.book.dto;

import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class BookResponseDto {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDetailDto {
        private String isbn13;
        private Long bookId;
        private String title;
        private String author;
        private String publisher;
        private String publicationDate;
        private String mallType;
        private MallType mallTypeCode;
        private String category;
        private Integer pages;
        private String description;
        private String coverImageUrl;
        private String aladinLink;
        private SourceType sourceType;
        private Long bookShelfId;
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSearchDTO { // 검색 결과 DTO
        private String isbn13;
        private String title;
        private String mallType;
        private String author;
        private String coverImageUrl;
        private String publisher;
        private String publicationDate;

        @Builder.Default
        private boolean isInLibrary = false; // 내 서재 등록 여부

        public void setInLibrary(boolean inLibrary) {
            isInLibrary = inLibrary;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultDto {
        private Long totalResults;
        private boolean hasNext;
        private Integer nextCursor;
        private List<BookSearchDTO> books;
    }
}
