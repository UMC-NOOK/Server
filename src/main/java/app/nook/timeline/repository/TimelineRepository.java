package app.nook.timeline.repository;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.Timeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface TimelineRepository extends JpaRepository<Timeline, Long> {
    void deleteByLibrary(Library library);

    List<Timeline> findTop5ByLibraryOrderByOccurredAtDescIdDesc(Library library);

    List<Timeline> findByLibraryOrderByOccurredAtDescIdDesc(Library library, Pageable pageable);

    @Query("""
            SELECT t FROM Timeline t
            WHERE t.library = :library
              AND (t.occurredAt < :occurredAt
                   OR (t.occurredAt = :occurredAt AND t.id < :timelineId))
            ORDER BY t.occurredAt DESC, t.id DESC
            """)
    List<Timeline> findBeforeCursor(
            @Param("library") Library library,
            @Param("occurredAt") LocalDateTime occurredAt,
            @Param("timelineId") Long timelineId,
            Pageable pageable
    );

    Optional<Timeline> findByIdAndLibrary(Long id, Library library);
}
