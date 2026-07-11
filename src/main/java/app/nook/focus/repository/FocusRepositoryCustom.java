package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.focus.repository.dto.FocusTimeStatsDto;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
import app.nook.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FocusRepositoryCustom {

    List<MonthlyFocusStatsDto> findMonthlyFocusStats(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<LocalDate> findDistinctFocusDatesByLibraryAndUser(
            Long libraryId,
            Long userId
    );

    List<FocusTimeStatsDto> findFocusTimeStats(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<FocusRangeStatsDto> findOverlappingFocusRanges(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    Slice<Focus> findByLibraryWithCursorByDate(
            User user,
            LocalDate focusDate,
            Long cursor,
            Pageable pageable
    );

    Slice<Focus> findRecentByUserWithCursor(
            User user,
            Long cursor,
            Pageable pageable
    );

    List<Focus> findRecentDistinctBooksByUser(
            User user,
            Pageable pageable
    );
}
