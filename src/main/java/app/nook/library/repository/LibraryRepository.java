package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryRepository extends JpaRepository<Library,Long>, LibraryRepositoryCustom{

    Library findByUserAndBook(User user, Book book);


    @Query("""
        select l
        from Library l
        where l.user = :user
          and l.readingStatus = :status
          and (:cursor is null or l.id < :cursor)
        order by l.id desc
    """)
    Slice<Library> findByStatusWithCursor(
            @Param("user") User user,
            @Param("status") ReadingStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    long countByUserAndReadingStatus(User user, ReadingStatus status);

}
