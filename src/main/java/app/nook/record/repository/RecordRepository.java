package app.nook.record.repository;

import app.nook.record.domain.Record;
import app.nook.record.dto.BookRecordDto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface RecordRepository extends JpaRepository<Record, Long>, RecordQueryRepository {
    @Query("""
        select count(r)
        from Record r
        where r.library.id = :libraryId
          and r.library.user.id = :userId
    """)
    long countByLibraryIdAndUserId(
            @Param("libraryId") Long libraryId,
            @Param("userId") Long userId
    );

    @Query("""
        select count(r)
        from Record r
        where r.library.user.id = :userId
        """)
    long countByUserId(@Param("userId") Long userId);

    long countByLibraryId(Long libraryId);

    @Query("""
        select r
        from Record r
        where r.library.id = :libraryId
        order by r.id desc
    """)
    List<Record> findRecentByLibraryId(
            @Param("libraryId") Long libraryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "images")
    Optional<Record> findWithImagesById(Long id);

    @EntityGraph(attributePaths = {"images", "library", "library.user"})
    Optional<Record> findWithDetailById(Long id);
}
