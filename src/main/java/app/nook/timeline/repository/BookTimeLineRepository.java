package app.nook.timeline.repository;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookTimeLineRepository extends JpaRepository<BookTimeLine, Long> {
    void deleteByLibrary(Library library);

    @Query("""
        select bt
        from BookTimeLine bt
        where bt.library = :library
          and bt.type <> :excludedType
          and (:cursor is null or bt.id < :cursor)
        order by bt.occurredAt desc, bt.id desc
    """)
    Slice<BookTimeLine> findPreviewByLibraryWithCursor(
            @Param("library") Library library,
            @Param("excludedType") BookTimeLineType excludedType,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    Optional<BookTimeLine> findByIdAndLibrary(Long id, Library library);
}
