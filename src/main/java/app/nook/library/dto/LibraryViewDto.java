package app.nook.library.dto;

import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.enums.ReadingStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class LibraryViewDto {

    public record BookResponseDto(
            Long bookId,
            String ISBN,
            String coverUrl
    ){}

    public record MonthlyBookResponseDto(
            LocalDate date,
            int bookNum,
            BookResponseDto book
    ){}

    public record MonthlyBookItem(
            YearMonth yearMonth,
            int totalBookNum,
            List<BookResponseDto> monthlyBookItems
    ){}

    public record DailyBookResponse(
            LocalDate localDate,
            CursorResponse<UserBookResponseDto> bookItems
    ){}

    public record UserBookResponseDto(
            Long bookId,
            String title,
            String author,
            int focusMin,
            String coverUrl
    ){}
    public record StatusBookResponseDto(
            ReadingStatus readingStatus,
            int totalBookNum,
            CursorResponse<? extends UserStatusBookItem> bookItems
    ) {}


    public sealed interface UserStatusBookItem
            permits BeforeBookItem, ReadingBookItem, FinishedBookItem {

        Long bookId();
        String title();
        String author();
        String coverUrl();
    }

    public record ReadingBookItem(
            Long bookId,
            String title,
            String author,
            String coverUrl,
            LocalDate startedAt
    ) implements UserStatusBookItem {}

    public record BeforeBookItem(
            Long bookId,
            String title,
            String author,
            String coverUrl
    ) implements UserStatusBookItem {}

    public record FinishedBookItem(
            Long bookId,
            String title,
            String author,
            String coverUrl,
            LocalDate startedAt,
            LocalDate endedAt
    ) implements UserStatusBookItem {}

}
