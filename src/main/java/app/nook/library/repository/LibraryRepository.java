package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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


    // [도서 상세 조회용] 사용자 ID + Book ID로 Library 조회
    Optional<Library> findByUserIdAndBookId(Long userId, Long bookId);

    // [전체 검색 - 서재 보유 여부 매핑용] ISBN 목록 중 서재에 있는 ISBN들 반환
    @Query("SELECT l.book.isbn13 FROM Library l WHERE l.user.id = :userId AND l.book.isbn13 IN :isbns")
    Set<String> findIsbnsByUserIdAndIsbnIn(@Param("userId") Long userId, @Param("isbns") List<String> isbns);

    // [서재 내 검색] 사용자의 서재에서 제목/저자/ISBN 키워드 검색
    @Query(value = "SELECT l FROM Library l JOIN FETCH l.book b JOIN FETCH b.category " +
            "WHERE l.user.id = :userId " +
            "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' " +
            "  OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' " +
            "  OR b.isbn13 LIKE CONCAT('%', :keyword, '%') ESCAPE '\\')",
            countQuery = "SELECT COUNT(l) FROM Library l JOIN l.book b " +
                    "WHERE l.user.id = :userId " +
                    "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' " +
                    "  OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' " +
                    "  OR b.isbn13 LIKE CONCAT('%', :keyword, '%') ESCAPE '\\')")
    Page<Library> searchByUserIdAndKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);

}
