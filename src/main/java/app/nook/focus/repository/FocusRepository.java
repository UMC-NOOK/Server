package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FocusRepository extends JpaRepository<Focus, Long> {

    interface FocusYearMonthProjection {
        Integer getYearValue();
        Integer getMonthValue();
    }

    interface MonthlyFocusStatsProjection {
        Integer getYearValue();
        Integer getMonthValue();
        Integer getDayValue();
        Long getBookId();
        String getCoverImageUrl();
        Long getTotalSec();
    }

    interface FocusTimeStatsProjection {
        Integer getYearValue();
        Integer getMonthValue();
        Integer getDayValue();
        Long getTotalSec();
    }

    interface FocusRangeProjection {
        LocalDateTime getStartedAt();
        LocalDateTime getEndedAt();
    }

    @Query("""
        select
            EXTRACT(YEAR FROM f.startedAt) as yearValue,
            EXTRACT(MONTH FROM f.startedAt) as monthValue,
            EXTRACT(DAY FROM f.startedAt) as dayValue,
            f.library.book.id as bookId,
            f.library.book.coverImageUrl as coverImageUrl,
            sum(f.durationSec) as totalSec
        from Focus f
        join f.library l
        where l.user.id = :userId
            and f.startedAt >= :start
            and f.startedAt < :end
        group by
            EXTRACT(YEAR FROM f.startedAt),
            EXTRACT(MONTH FROM f.startedAt),
            EXTRACT(DAY FROM f.startedAt),
            f.library.book.id,
            f.library.book.coverImageUrl
        order by sum(f.durationSec) desc
    """)
    List<MonthlyFocusStatsProjection> findMonthlyFocusStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select distinct
            EXTRACT(YEAR FROM f.startedAt) as yearValue,
            EXTRACT(MONTH FROM f.startedAt) as monthValue
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
            EXTRACT(YEAR FROM f.startedAt) as yearValue,
            EXTRACT(MONTH FROM f.startedAt) as monthValue,
            EXTRACT(DAY FROM f.startedAt) as dayValue,
            sum(f.durationSec) as totalSec
       from Focus f
       join f.library l
       where l.user.id = :userId
         and f.startedAt >= :start
         and f.startedAt < :end
       group by
            EXTRACT(YEAR FROM f.startedAt),
            EXTRACT(MONTH FROM f.startedAt),
            EXTRACT(DAY FROM f.startedAt)
    """)
    List<FocusTimeStatsProjection> findFocusTimeStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
       select
            f.startedAt as startedAt,
            f.endedAt as endedAt
       from Focus f
       join f.library l
       where l.user.id = :userId
         and f.endedAt is not null
         and f.startedAt < :end
         and f.endedAt > :start
    """)
    List<FocusRangeProjection> findOverlappingFocusRanges(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
