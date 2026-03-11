package app.nook.timeline.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;
import app.nook.timeline.converter.TimelineResponseConverter;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineQueryService {

    private static final int DEFAULT_TIMELINE_PREVIEW_SIZE = 5;

    private final LibraryRepository libraryRepository;
    private final FocusRepository focusRepository;
    private final BookTimeLineRepository bookTimeLineRepository;

    // TODO: 추후 기능 추가 예정

    public TimelineResponseDto.TimelineSummaryDto getTimelineSummary(User user, Long libraryId) {
        Library library = getOwnedLibrary(user, libraryId);

        TimelineResponseDto.FocusSummaryDto focusSummary =
                new TimelineResponseDto.FocusSummaryDto(
                        library.getStartedAt(),
                        library.getEndedAt(),
                        library.getFocusSec(),
                        focusRepository.countByLibrary(library),
                        library.getPage()
                );

        TimelineResponseDto.RecordSummaryDto recordSummary =
                new TimelineResponseDto.RecordSummaryDto(
                        0,
                        null
                );

        TimelineResponseDto.TimelinePreviewDto timelinePreview =
                getTimelinePreview(user, libraryId, null, DEFAULT_TIMELINE_PREVIEW_SIZE);

        return TimelineResponseConverter.toTimelineSummary(
                library.getId(),
                focusSummary,
                recordSummary,
                timelinePreview
        );
    }

    public TimelineResponseDto.TimelinePreviewDto getTimelinePreview(
            User user,
            Long libraryId,
            Long cursor,
            int size
    ) {
        Library library = getOwnedLibrary(user, libraryId);
        Pageable pageable = PageRequest.of(0, size + 1);

        Slice<BookTimeLine> timelines = bookTimeLineRepository.findPreviewByLibraryWithCursor(
                library,
                BookTimeLineType.STATUS,
                cursor,
                pageable
        );

        List<BookTimeLine> content = timelines.getContent();
        boolean hasNext = content.size() > size;
        List<BookTimeLine> pageContent = hasNext
                ? content.subList(0, size)
                : content;

        BookTimeLine previousTimeline = cursor == null
                ? null
                : bookTimeLineRepository.findByIdAndLibrary(cursor, library).orElse(null);

        List<TimelineResponseDto.TimelineItemDto> items = new ArrayList<>();
        for (BookTimeLine timeline : pageContent) {
            boolean showYear = previousTimeline == null
                    || previousTimeline.getOccurredAt().getYear() != timeline.getOccurredAt().getYear();

            items.add(toTimelineItem(timeline, showYear));
            previousTimeline = timeline;
        }

        Long nextCursor = hasNext && !pageContent.isEmpty()
                ? pageContent.get(pageContent.size() - 1).getId()
                : null;

        return TimelineResponseConverter.toTimelinePreview(
                items,
                nextCursor,
                hasNext
        );
    }

    private Library getOwnedLibrary(User user, Long libraryId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        if (!library.getUser().getId().equals(user.getId())) {
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        return library;
    }

    private TimelineResponseDto.TimelineItemDto toTimelineItem(BookTimeLine timeline, boolean showYear) {
        return switch (timeline.getType()) {
            case REGISTER -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    showYear,
                    "서재에 등록했어요",
                    null,
                    List.of(TimelineResponseDto.TimelineActionType.REMOVE_FROM_LIBRARY)
            );
            case FOCUS -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    showYear,
                    "포커스",
                    null,
                    List.of(TimelineResponseDto.TimelineActionType.VIEW_DETAIL)
            );
            case RECORD -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    showYear,
                    "독서 기록",
                    null,
                    List.of(TimelineResponseDto.TimelineActionType.VIEW_DETAIL)
            );
            case STATUS -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    showYear,
                    "독서 상태 변경",
                    null,
                    List.of()
            );
        };
    }
}
