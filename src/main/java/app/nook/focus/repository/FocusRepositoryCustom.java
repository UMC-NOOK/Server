package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FocusRepositoryCustom {

    List<LocalDate> findDistinctFocusDatesByLibraryAndUser(
            Long libraryId,
            Long userId
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
