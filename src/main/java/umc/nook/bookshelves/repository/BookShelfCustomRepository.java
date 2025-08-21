package umc.nook.bookshelves.repository;


import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.dto.SortType;
import umc.nook.users.domain.User;

import java.time.YearMonth;
import java.util.List;

public interface BookShelfCustomRepository {
    BookShelfDTO.PageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user, ReadingStatus status, int page, int size, SortType sort);

    List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(
            User user, YearMonth yearMonth);

    BookShelfDTO.BooksInsightDTO getBooksInsight(User user);

}
