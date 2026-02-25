package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FocusRepository extends JpaRepository<Focus, Long> {

    interface FocusYearMonthProjection {
        Integer getYearValue();
        Integer getMonthValue();
    }

    interface MonthlyFocusStatsProjection {
        LocalDate getDateValue();
        Long getBookId();
        String getCoverImageUrl();
        Long getTotalSec();
    }

    interface FocusTimeStatsProjection {
        LocalDate getDateValue();
        Long getTotalSec();
    }

    @Query("""
        select
            FUNCTION('DATE', f.startedAt) as dateValue,
            f.library.book.id as bookId,
            f.library.book.coverImageUrl as coverImageUrl,
            sum(f.durationSec) as totalSec
        from Focus f
        join f.library l
        where l.user.id = :userId
            and f.startedAt >= :start
            and f.startedAt < :end
        group by FUNCTION('DATE', f.startedAt), f.library.book.id, f.library.book.coverImageUrl
        order by sum(f.durationSec) desc
    """)
    List<MonthlyFocusStatsProjection> findMonthlyFocusStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select distinct
            YEAR(f.startedAt) as yearValue,
            MONTH(f.startedAt) as monthValue
        from Focus f
        where f.library.id = :libraryId
          and f.library.user.id = :userId
    """)
    List<FocusYearMonthProjection> findDistinctFocusYearMonthsByLibraryAndUser(
            @Param("libraryId") Long libraryId,
            @Param("userId") Long userId
    );


    // 포커스 통계 조회
    @Query("""
       select
            FUNCTION('DATE', f.startedAt) as dateValue,
            sum(f.durationSec) as totalSec
       from Focus f
       join f.library l
       where l.user.id = :userId
         and f.startedAt >= :start
         and f.startedAt < :end
       group by FUNCTION('DATE', f.startedAt)
    """)
    List<FocusTimeStatsProjection> findFocusTimeStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
