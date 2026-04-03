package app.nook.timeline.converter;

import app.nook.timeline.domain.Timeline;
import app.nook.timeline.dto.TimelineResponseDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimelineResponseConverter {

    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

    private TimelineResponseConverter() {}

    public static TimelineResponseDto.TimelineDateGroupDto toTimelineDateGroup(
            LocalDate date,
            boolean showYear
    ) {
        return new TimelineResponseDto.TimelineDateGroupDto(
                date.getYear(),
                date.format(MONTH_DAY_FORMATTER),
                showYear,
                List.of()
        );
    }

    public static TimelineResponseDto.TimelineItemDto toTimelineItem(
            Timeline timeline,
            String title,
            String subtitle
    ) {
        return new TimelineResponseDto.TimelineItemDto(
                timeline.getId(),
                timeline.getType(),
                timeline.getOccurredAt(),
                title,
                subtitle,
                timeline.getPreviewText(),
                timeline.getTargetId()
        );
    }

    public static TimelineResponseDto.TimelinePreviewDto toTimelinePreview(
            List<TimelineResponseDto.TimelineDateGroupDto> dateGroups
    ) {
        return new TimelineResponseDto.TimelinePreviewDto(dateGroups);
    }

    public static TimelineResponseDto.TimelineDateGroupDto toTimelineDateGroup(
            LocalDate date,
            boolean showYear,
            List<TimelineResponseDto.TimelineItemDto> items
    ) {
        return new TimelineResponseDto.TimelineDateGroupDto(
                date.getYear(),
                date.format(MONTH_DAY_FORMATTER),
                showYear,
                items
        );
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

    public static TimelineResponseDto.TimelineDetailDto toTimelineDetail(
            Timeline timeline,
            TimelineResponseDto.TimelineDetail detail
    ) {
        return new TimelineResponseDto.TimelineDetailDto(
                timeline.getId(),
                timeline.getType(),
                timeline.getOccurredAt(),
                detail
        );
    }

    public static TimelineResponseDto.TimelineRegisterDetailDto toRegisterDetail(String description) {
        return new TimelineResponseDto.TimelineRegisterDetailDto(description);
    }

    public static TimelineResponseDto.TimelineStatusDetailDto toStatusDetail(String title, String description) {
        return new TimelineResponseDto.TimelineStatusDetailDto(title, description);
    }

    public static TimelineResponseDto.TimelineFocusDetailDto toFocusDetail(String timeText, Integer page) {
        return new TimelineResponseDto.TimelineFocusDetailDto(timeText, page);
    }

    public static TimelineResponseDto.TimelineRecordDetailDto toRecordDetail(
            String content,
            String emotion,
            List<String> imageUrls
    ) {
        return new TimelineResponseDto.TimelineRecordDetailDto(content, emotion, imageUrls);
    }
}
