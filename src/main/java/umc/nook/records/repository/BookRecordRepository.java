package umc.nook.records.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.BookRecord;

import java.util.List;
import java.util.Optional;

public interface BookRecordRepository extends JpaRepository<BookRecord,Long> , RecordCustomRepository{
    List<BookRecord> findAllByBookshelfAndParentIsNullOrderByCreatedDateAsc(UserBookShelf userBookShelf);

    List<BookRecord> findAllByParent(BookRecord record);

    void deleteAllByBookshelf(UserBookShelf userBook);
}
