package umc.nook.book.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.book.domain.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

}
