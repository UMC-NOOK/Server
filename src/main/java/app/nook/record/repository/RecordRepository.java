package app.nook.record.repository;

import app.nook.record.domain.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordRepository extends JpaRepository<Record, Long> {

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
}
