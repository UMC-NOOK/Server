package app.nook.focus.service;

import app.nook.focus.converter.FocusConverter;
import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryQueryService;
import app.nook.library.util.FocusTimeUtil;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusQueryService {

    private final FocusRepository focusRepository;
    private final PresignedUrlService presignedUrlService;
    private final LibraryQueryService libraryQueryService;
    private final Clock clock;

    public CursorResponse<FocusResponseDto.RecentFocusItem, Long> getRecentFocuses(
            User user,
            Long cursor,
            int size
    ) {
        Slice<Focus> slice = focusRepository.findRecentByUserWithCursor(
                user, cursor, PageRequest.of(0, size)
        );

        List<Focus> content = slice.getContent();
        Long nextCursor = slice.hasNext() ? content.get(content.size() - 1).getId() : null;

        List<FocusResponseDto.RecentFocusItem> items = content.stream()
                .map(focus -> FocusConverter.toRecentFocusItem(
                        focus,
                        presignedUrlService.resolveImageUrl(
                                user.getId(),
                                focus.getLibrary().getBook().getCoverImageKey()
                        )
                ))
                .toList();

        return CursorResponse.of(items, nextCursor, slice.hasNext());
    }

    public FocusResponseDto.HomeResponse getFocusHome(
            User user,
            ReadingStatus status,
            Long cursor,
            int size
    ) {
        LocalDate today = LocalDate.now(clock);
        List<MonthlyFocusStatsDto> dailyFocusStats = focusRepository.findMonthlyFocusStats(
                user.getId(), today, today.plusDays(1)
        );

        Map<Long, Long> focusSecondsByBook = new HashMap<>();
        long totalFocusSeconds = 0;
        for (MonthlyFocusStatsDto dailyFocusStat : dailyFocusStats) {
            long focusSeconds = dailyFocusStat.getTotalSec() == null ? 0 : dailyFocusStat.getTotalSec();
            focusSecondsByBook.merge(dailyFocusStat.getBookId(), focusSeconds, Long::sum);
            totalFocusSeconds += focusSeconds;
        }

        LibraryViewDto.StatusBookResponseDto booksByStatus = libraryQueryService.getBooksByStatus(
                user.getId(), status, cursor, size
        );
        CursorResponse<FocusResponseDto.HomeBookItem, Long> books = CursorResponse.of(
                booksByStatus.bookItems().items().stream()
                        .map(book -> new FocusResponseDto.HomeBookItem(
                                book.bookId(),
                                book.title(),
                                book.author(),
                                book.coverUrl(),
                                formatFocusTime(focusSecondsByBook.getOrDefault(book.bookId(), 0L))
                        ))
                        .toList(),
                booksByStatus.bookItems().nextCursor(),
                booksByStatus.bookItems().hasNext()
        );

        return new FocusResponseDto.HomeResponse(
                formatFocusTime(totalFocusSeconds),
                status,
                books
        );
    }

    private String formatFocusTime(long focusSeconds) {
        return FocusTimeUtil.formatFocusTime(focusSeconds);
    }
}
