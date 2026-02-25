package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn13(String isbn13);

    Optional<Book> findByIsbn13AndSourceType(String isbn13, SourceType sourceType);
}
