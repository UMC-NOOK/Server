package umc.nook.bookshelves.service;

import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.users.domain.User;

import java.time.YearMonth;
import java.util.List;

public interface BookshelfService {


    public String registerBook(BookShelfDTO.RegisterBookDTO registerBookDTO, User user);

    public String deleteBook(Long bookId, User user);

    public String changeBookState(Long bookId, User user);

    List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth);

    BookShelfDTO.CursorPageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user, String status, Long cursorBookId, int size, String sort
    );
}
