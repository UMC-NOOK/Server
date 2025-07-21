package umc.nook.records.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.ChatRecord;

import java.util.List;

public interface ChatRecordRepository extends JpaRepository<ChatRecord,Long> {
    List<ChatRecord> findByBookshelfIdOrderByCreatedAt(Long bookshelfId);

    List<ChatRecord> findByBookshelfOrderByCreatedAtAsc(UserBookShelf userBook);
}
