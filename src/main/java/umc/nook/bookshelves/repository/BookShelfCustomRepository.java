package umc.nook.bookshelves.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;

public interface BookShelfCustomRepository {
    List<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user, ReadingStatus status, Long cursorBookId, int size, String sort);

    List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(
            User user, YearMonth yearMonth);

    BookShelfDTO.BooksInsightDTO getBooksInsight(User user);

    RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year);
}
