package umc.nook.bookshelves.repository;

import com.querydsl.core.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.book.domain.Book;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.users.domain.User;

import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.book.domain.CategoryCount;
import umc.nook.book.domain.CategoryCountByName;

public interface UserBookshelfRepository extends JpaRepository<UserBookShelf,Long> {
    boolean existsByUserAndBook(User user, Book book);

    UserBookShelf findByUserAndBook(User user, Book book);

    void deleteByUserAndBook(User user, Book thisBook);

    List<UserBookShelf> findByUserAndReadingStatus(User user, ReadingStatus readingStatus);

    UserBookShelf findByUser(User user);

    List<UserBookShelf> findAllByUser(User user);

    @Query("""
    SELECT c.categoryId AS categoryId,
           c.aladinCategoryId AS aladinCategoryId,
           c.categoryName AS categoryName,
           COUNT(ubs) AS count
    FROM UserBookShelf ubs
    JOIN ubs.book b
    JOIN b.category c
    WHERE ubs.user.id = :userId
      AND ubs.readingStatus IN (umc.nook.bookshelves.domain.ReadingStatus.READING, umc.nook.bookshelves.domain.ReadingStatus.FINISHED)
    GROUP BY c.categoryId, c.aladinCategoryId, c.categoryName
    ORDER BY count DESC
""")
    List<CategoryCount> findCategoryCountByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
    SELECT c.categoryId AS categoryId,
           c.aladinCategoryId AS aladinCategoryId,
           c.categoryName AS categoryName,
           COUNT(ubs) AS count
    FROM UserBookShelf ubs
    JOIN ubs.book b
    JOIN b.category c
    WHERE ubs.readingStatus IN (umc.nook.bookshelves.domain.ReadingStatus.READING, umc.nook.bookshelves.domain.ReadingStatus.FINISHED)
    GROUP BY c.categoryId, c.aladinCategoryId, c.categoryName
    ORDER BY count DESC
""")
    List<CategoryCount> findCategoryCountGlobal(Pageable pageable);

    @Query("""
    SELECT c.categoryName AS categoryName,
           COUNT(ubs) AS count
    FROM UserBookShelf ubs
    JOIN ubs.book b
    JOIN b.category c
    WHERE ubs.user.id = :userId
      AND ubs.readingStatus IN (umc.nook.bookshelves.domain.ReadingStatus.READING, umc.nook.bookshelves.domain.ReadingStatus.FINISHED)
    GROUP BY c.categoryName
    ORDER BY count DESC
""")
    List<CategoryCountByName> findCategoryCountByUserIdGroupByName(@Param("userId") Long userId, Pageable pageable);

    Long countByUser_UserId(Long userId);
}
