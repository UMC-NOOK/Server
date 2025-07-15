package umc.nook.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reactor.core.publisher.Mono;
import umc.nook.book.domain.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    Book findByIsbn13(String isbn13);

    Book findByBookId(Long bookId);
}
