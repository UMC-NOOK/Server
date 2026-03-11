package app.nook.timeline.converter;

import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.dto.TimelineResponseDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimelineResponseConverter {

    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

    private TimelineResponseConverter() {}

    public static TimelineResponseDto.TimelineDisplayDateDto toDisplayDate(
            LocalDateTime occurredAt,
            boolean showYear
    ) {
        return new TimelineResponseDto.TimelineDisplayDateDto(
                occurredAt.getYear(),
                occurredAt.format(MONTH_DAY_FORMATTER),
                showYear
        );
    }

    public static TimelineResponseDto.TimelineItemDto toTimelineItem(
            BookTimeLine timeline,
            boolean showYear,
            String title,
            String subtitle,
            List<TimelineResponseDto.TimelineActionType> actions
    ) {
        return new TimelineResponseDto.TimelineItemDto(
                timeline.getId(),
                timeline.getType(),
                timeline.getOccurredAt(),
                toDisplayDate(timeline.getOccurredAt(), showYear),
                title,
                subtitle,
                timeline.getPreviewText(),
                timeline.getTargetId(),
                actions
        );
    }

    public static TimelineResponseDto.TimelinePreviewDto toTimelinePreview(
            List<TimelineResponseDto.TimelineItemDto> items,
            Long nextCursor,
            boolean hasNext
    ) {
        return new TimelineResponseDto.TimelinePreviewDto(items, nextCursor, hasNext);
    }

    public static TimelineResponseDto.TimelineSummaryDto toTimelineSummary(
            Long libraryId,
            TimelineResponseDto.FocusSummaryDto focusSummary,
            TimelineResponseDto.RecordSummaryDto recordSummary,
            TimelineResponseDto.TimelinePreviewDto timelinePreview
    ) {
        return new TimelineResponseDto.TimelineSummaryDto(
                libraryId,
                focusSummary,
                recordSummary,
                timelinePreview
        );
    }
}
