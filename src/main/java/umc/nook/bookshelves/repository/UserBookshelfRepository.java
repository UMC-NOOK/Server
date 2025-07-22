package umc.nook.bookshelves.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.book.domain.Book;
import umc.nook.book.domain.CategoryCount;
import umc.nook.book.domain.CategoryCountByName;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.users.domain.User;

import java.util.List;

public interface UserBookshelfRepository extends JpaRepository<UserBookShelf,Long> {
    boolean existsByUserAndBook(User user, Book book);

    UserBookShelf findByUserAndBook(User user, Book book);

    void deleteByUserAndBook(User user, Book thisBook);

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
