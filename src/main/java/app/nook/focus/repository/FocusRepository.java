package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import app.nook.user.domain.User;
import org.springframework.data.domain.Pageable;
import app.nook.library.domain.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FocusRepository extends JpaRepository<Focus, Long>, FocusRepositoryCustom {
    @Query("""
        select f
        from Focus f
        join fetch f.library l
        join fetch l.book
        where l.user = :user
        order by f.id desc
    """)
    List<Focus> findRecentByUser(@Param("user") User user, Pageable pageable);
    List<Focus> findAllByLibraryIdAndLibraryUserId(Long libraryId, Long userId);
    Optional<Focus> findByLibraryUserIdAndEndedAtIsNull(Long userId);
    Optional<Focus> findByIdAndLibraryUserId(Long focusId, Long userId);
    int countByLibrary(Library library);
}
