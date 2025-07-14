package umc.nook.bookshelves.repository;

import com.querydsl.core.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.books.domain.Book;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.users.domain.User;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface UserBookshelfRepository extends JpaRepository<UserBookShelf,Long> {
    boolean existsByUserAndBook(User user, Book book);

    UserBookShelf findByUserAndBook(User user, Book book);

    public List<Tuple> findUserBooksGroupedByDate(Long userId, YearMonth yearMonth);
}
