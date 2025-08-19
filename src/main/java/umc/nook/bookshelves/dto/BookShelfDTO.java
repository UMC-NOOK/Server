package umc.nook.bookshelves.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.book.domain.Book;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.validation.DateRequiredIfReadingOrFinished;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class BookShelfDTO {

    @Getter
    @NoArgsConstructor
    @DateRequiredIfReadingOrFinished
    public static class RegisterBookDTO {

        @NotNull(message = "bookId는 필수입니다.")
        @Positive(message = "bookId는 양수여야 합니다.")
        private Long bookId;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @NotNull(message = "readingStatus는 필수입니다.")
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

        private String isbn13;
        private int myRating;

        private String publicationDate;

        @QueryProjection
        public UserBookListResponseDTO(Long bookId, String title, String author,
                                    String publisher, String coverImageUrl,
                                    String readingStatus, String isbn13,int myRating,String publicationDate) {
            this.bookId = bookId;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.coverImageUrl = coverImageUrl;
            this.readingStatus = readingStatus;
            this.isbn13 = isbn13;
            this.myRating = myRating;
            this.publicationDate = publicationDate;
        }
    }


    @Getter
    @AllArgsConstructor
    public static class BookThumbnail {
        private Long bookId;
        private String title;
        private String thumbnailUrl;

        public BookThumbnail(Book book) {
            this.bookId = book.getBookId();
            this.title = book.getTitle();
            this.thumbnailUrl = book.getCoverImageUrl();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class MonthlyBookThumbnail {
        private Long bookId;
        private String title;
        private String thumbnailUrl;
        private String author;

        public MonthlyBookThumbnail(Book book) {
            this.bookId = book.getBookId();
            this.title = book.getTitle();
            this.thumbnailUrl = book.getCoverImageUrl();
            this.author = book.getAuthor();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DailyBooksResponseDTO {
        private LocalDate date;
        private MonthlyBookThumbnail bookInfo;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PageDTO<T> {
        private List<T> content;  // 데이터 목록
        private int page;         // 현재 페이지 번호 (0부터 시작)
        private int size;         // 페이지 크기
        private boolean hasNext;  // 다음 페이지 존재 여부
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisteredBookListResponseDTO {
        private List<LocalDate> dates;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BooksInsightDTO {
        private Long totalBookCount;
        private Long totalRecordCount;
        private List<BooksInsightTypeDTO> statusCounts;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BooksInsightTypeDTO {
        private ReadingStatus readingStatus;
        private int bookCount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeeklyBooksDTO {
        int day;

        BookThumbnail bookInfo;

    }


}
