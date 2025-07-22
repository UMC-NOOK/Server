package umc.nook.records.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.BookRecord;

import java.util.List;

public interface BookRecordRepository extends JpaRepository<BookRecord,Long> {
    List<BookRecord> findAllByBookshelfAndParentIsNullOrderByCreatedDateAsc(UserBookShelf userBookShelf);

    List<BookRecord> findAllByParent(BookRecord record);
}
