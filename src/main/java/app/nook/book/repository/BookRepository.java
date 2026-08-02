package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn13(String isbn13);

    Optional<Book> findByIsbn13AndSourceType(String isbn13, SourceType sourceType);

    /** 고아 이미지 대조용 — 사용 중인 모든 책 커버 이미지 key */
    @Query("SELECT b.coverImageKey FROM Book b WHERE b.coverImageKey IS NOT NULL")
    List<String> findAllCoverImageKeys();
}
