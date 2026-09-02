package app.nook.focus.converter;

import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.library.util.FocusTimeUtil;

import java.time.LocalDateTime;

public class FocusConverter {

    public static FocusResponseDto.FocusStart toFocusStartResponse(Focus focus) {
        return new FocusResponseDto.FocusStart(
                focus.getId(),
                focus.getLibrary().getBook().getId(),
                focus.getLibrary().getBook().getTitle(),
                focus.getLibrary().getBook().getAuthor(),
                focus.getStartedAt()
        );
    }

    public static FocusResponseDto.FocusEnd toFocusEndResponse(Focus focus) {
        return new FocusResponseDto.FocusEnd(
                focus.getId(),
                focus.getLibrary().getBook().getId(),
                focus.getStartedAt(),
                focus.getEndedAt(),
                focus.getDurationSec(),
                FocusTimeUtil.formatFocusTime(focus.getDurationSec() == null ? 0 : focus.getDurationSec()),
                toNullablePage(focus.getLibrary().getPage()),
                focus.getLibrary().getFocusSec(),
                focus.getLibrary().getReadingStatus().name()
        );
    }

    public static FocusResponseDto.FocusEnd toFocusEndResponse(
            Focus originalFocus,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int durationSec
    ) {
        return new FocusResponseDto.FocusEnd(
                originalFocus.getId(),
                originalFocus.getLibrary().getBook().getId(),
                startedAt,
                endedAt,
                durationSec,
                FocusTimeUtil.formatFocusTime(durationSec),
                toNullablePage(originalFocus.getLibrary().getPage()),
                originalFocus.getLibrary().getFocusSec(),
                originalFocus.getLibrary().getReadingStatus().name()
        );
    }

    private static Integer toNullablePage(int page) {
        return page == 0 ? null : page;
    }

    public static FocusResponseDto.RecentFocusItem toRecentFocusItem(Focus focus, String coverImageUrl) {
        return new FocusResponseDto.RecentFocusItem(
                focus.getId(),
                focus.getLibrary().getBook().getId(),
                focus.getLibrary().getBook().getTitle(),
                focus.getLibrary().getBook().getAuthor(),
                coverImageUrl,
                focus.getStartedAt(),
                focus.getEndedAt(),
                FocusTimeUtil.formatFocusTime(focus.getDurationSec() == null ? 0 : focus.getDurationSec())
        );
    }
}
