package app.nook.book.dto;

import app.nook.book.entity.MallType;
import app.nook.book.entity.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private MallType mallType;
        private String category;
        private int pages;
        private String description;
        private String coverImageUrl;
        private String aladinLink;
        private SourceType sourceType;
        private Long bookShelfId;
    }
}
