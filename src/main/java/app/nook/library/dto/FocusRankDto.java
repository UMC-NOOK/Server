package app.nook.library.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class FocusRankDto {

    // 포커스 전체 통계
    public record FocusBookResponseDto(
            YearMonth yearMonth,
            int totalFocusMin,
            List<FocusDateItem> focusBookItems
    ){}

    // 포커스 랭킹 통계
    public record FocusDateItem(
            LocalDate date,
            FocusTimeSlot timeSlot
    ){}

    // 월 통계
    public record MonthlyBooksResponseDto(
            YearMonth yearMonth,
            int totalBookCount,
            List<DailyBookItem> days
    ){}

    // 통계 시 책 아이템
    public record DailyBookItem(
            LocalDate date,
            Long bookCount,
            BookCalendarInfo topBook
    ){}

    public record BookCalendarInfo(
            Long bookId,
            String coverUrl
    ){}

    public record BookDetailInfo(
            Long bookId,
            String coverUrl,
            String title,
            String author,
            Long focusSec
    ) {}

    // 중간 row dto
    public record MonthlyFocusRow(
            LocalDate date,
            Long bookId,
            String coverImageUrl,
            Long totalSec
    ){}

    // 중간 focus row dto
    public record FocusTimeRow(
            LocalDate date,
            Long totalSec
    ){}


}
