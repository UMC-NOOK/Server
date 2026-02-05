package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.library.domain.Library;
import app.nook.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library,Long> {

    Library findByUserAndBook(User user, Book book);
}
