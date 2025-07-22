package umc.nook.bookshelves.repository;

import com.querydsl.core.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.book.domain.Book;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.users.domain.User;

import java.time.YearMonth;
import java.util.List;

public interface UserBookshelfRepository extends JpaRepository<UserBookShelf,Long> {
    boolean existsByUserAndBook(User user, Book book);

    UserBookShelf findByUserAndBook(User user, Book book);

    void deleteByUserAndBook(User user, Book thisBook);

    UserBookShelf findByUser(User user);

    List<UserBookShelf> findAllByUser(User user);
}
