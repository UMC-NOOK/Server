package app.nook.timeline.dto;

import app.nook.timeline.domain.enums.BookTimeLineType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimelineResponseDto {

    public record TimelineSummaryDto(
            Long libraryId,
            FocusSummaryDto focusSummary,
            RecordSummaryDto recordSummary,
            TimelinePreviewDto timelinePreview
    ) {}

    public record FocusSummaryDto(
            LocalDate startedAt,
            LocalDate endedAt,
            Long totalFocusSec,
            int focusCount,
            Integer page
    ) {}

    public record RecordSummaryDto(
            int recordCount,
            String latestRecordPreview
    ) {}

    public record TimelinePreviewDto(
            List<TimelineItemDto> items,
            Long nextCursor,
            boolean hasNext
    ) {}

    public record TimelineItemDto(
            Long timelineId,
            BookTimeLineType type,
            LocalDateTime occurredAt,
            TimelineDisplayDateDto displayDate,
            String title,
            String subtitle,
            String previewText,
            Long targetId,
            List<TimelineActionType> actions
    ) {}

    public record TimelineDisplayDateDto(
            Integer year,
            String monthDay,
            boolean showYear
    ) {}

    public enum TimelineActionType {
        VIEW_DETAIL,
        REMOVE_FROM_LIBRARY
    }
}
