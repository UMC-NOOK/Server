package app.nook.book.dto;

import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusResponse;
import lombok.*;

import java.util.List;

public class BookResponseDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
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
        private Long libraryId;
        private ReadingStatusResponse readingStatus;
    }

    public record BookPreviewDto( // 검색 베스트셀러 프리뷰 DTO
            String isbn13,
            String title,
            String author,
            String coverImageUrl,
            String publisher,
            Integer rank
    ) {}

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    public static class BookSearchDto { // 검색 결과 DTO
        private Long bookId;
        private String isbn13;
        private String title;
        private String mallType;
        private String author;
        private String coverImageUrl;
        private String publisher;
        private String publicationDate;

        @Setter
        @Builder.Default
        private boolean inLibrary = false; // 내 서재 등록 여부

        private ReadingStatus readingStatus; // 전체 검색시 null
    }

    public record SearchResultDto(
            Long totalResults,
            boolean hasNext,
            Integer nextCursor,
            List<BookSearchDto> books
    ) {}
}
