package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookViewHistoryRepository extends JpaRepository<BookViewHistory, Long> {

    @Query("SELECT bvh FROM BookViewHistory bvh WHERE bvh.user = :user AND bvh.book = :book")
    Optional<BookViewHistory> findExisting(@Param("user") User user, @Param("book") Book book);

    @Query("SELECT bvh FROM BookViewHistory bvh JOIN FETCH bvh.book WHERE bvh.user = :user ORDER BY bvh.modifiedDate DESC, bvh.id DESC")
    List<BookViewHistory> findAllRecent(@Param("user") User user, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bvh FROM BookViewHistory bvh WHERE bvh.user = :user ORDER BY bvh.modifiedDate DESC, bvh.id DESC")
    List<BookViewHistory> findAllRecentForUpdate(@Param("user") User user);
}
