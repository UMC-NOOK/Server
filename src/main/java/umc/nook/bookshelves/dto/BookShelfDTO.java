package umc.nook.bookshelves.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.bookshelves.domain.ReadingStatus;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class BookShelfDTO {

    @Getter
    @NoArgsConstructor
    public static class RegisterBookDTO {
        private Long bookId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        private ReadingStatus readingStatus;
    }


    @Getter
    @NoArgsConstructor
    public static class UserBookListResponseDTO {

        private Long bookId;
        private String title;
        private String author;
        private String publisher;
        private String coverImageUrl;
        private String readingStatus;
        private int myRating;

        @QueryProjection
        public UserBookListResponseDTO(Long bookId, String title, String author,
                                    String publisher, String coverImageUrl,
                                    String readingStatus, int myRating) {
            this.bookId = bookId;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.coverImageUrl = coverImageUrl;
            this.readingStatus = readingStatus;
            this.myRating = myRating;
        }
    }

    public enum SortOption {
        title, latest, recent, rating
    }


    @Getter
    @AllArgsConstructor
    public static class BookThumbnail {
        private Long bookId;
        private String thumbnailUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class DailyBooksResponseDTO {
        private LocalDate date;
        private List<BookThumbnail> books;
    }

    @Getter
    @AllArgsConstructor
    public static class CursorPageDTO<T> {
        private List<T> content;
        private Long nextCursor;
        private boolean hasNext;
    }


}
