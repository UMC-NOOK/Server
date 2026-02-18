package app.nook.book.repository;

import app.nook.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn13(String isbn13);
}
