package umc.nook.book.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.book.converter.BookConverter;
import umc.nook.review.dto.ReviewResponseDTO;

import java.util.List;

public class BookResponseDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDetailDTO{
        private Long bookId;
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String publicationDate;
        private String mallType;
        private String category;
        private Integer pages;
        private String description;
        private String coverImageUrl;
        private boolean registeredBookshelf;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BestInThisCategoryDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String coverImageUrl;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDetailResultDTO{
        private BookDetailDTO book;
        private ReviewResponseDTO.ReviewResultDTO reviewData;
        private List<BestInThisCategoryDTO> bestInThisCategory;
    }

}
