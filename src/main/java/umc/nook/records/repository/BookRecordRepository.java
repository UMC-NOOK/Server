package umc.nook.records.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.BookRecord;

import java.util.List;
import java.util.Optional;

public interface BookRecordRepository extends JpaRepository<BookRecord,Long> {
    List<BookRecord> findAllByBookshelfAndParentIsNullOrderByCreatedDateAsc(UserBookShelf userBookShelf);

    List<BookRecord> findAllByParent(BookRecord record);

    @Query(value = """
        SELECT b.book_id AS bookId,
               b.title AS title,
               b.cover_image_url AS coverImageUrl
        FROM (
            SELECT br.created_date, bs.book_id
            FROM book_record br
            JOIN bookshelf bs ON br.bookshelf_id = bs.bookshelf_id
            WHERE bs.user_id = :userId

            UNION ALL

            SELECT cr.created_date, bs.book_id
            FROM chat_record cr
            JOIN bookshelf bs ON cr.bookshelf_id = bs.bookshelf_id
            WHERE bs.user_id = :userId
        ) AS recent
        JOIN book b ON b.book_id = recent.book_id
        ORDER BY recent.created_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<RecentRecordProjection> findMostRecentBookByUserId(@Param("userId") Long userId);
}
