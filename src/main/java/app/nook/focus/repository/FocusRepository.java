package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import app.nook.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import app.nook.library.domain.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    Optional<Focus> findByLibraryUserIdAndEndedAtIsNull(Long userId);
    Optional<Focus> findByIdAndLibraryUserId(Long focusId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select f
        from Focus f
        join f.library l
        where f.id = :focusId
          and l.user.id = :userId
    """)
    Optional<Focus> findByIdAndLibraryUserIdForUpdate(
            @Param("focusId") Long focusId,
            @Param("userId") Long userId
    );

    @Query("select f.library.id from Focus f where f.id = :focusId")
    Optional<Long> findLibraryIdById(@Param("focusId") Long focusId);

    List<Focus> findByLibraryAndSessionIdOrderByIdAsc(Library library, UUID sessionId);

    int countByLibrary(Library library);
}
