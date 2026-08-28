package app.nook.timeline.dto;

import app.nook.timeline.domain.enums.TimelineType;
import com.fasterxml.jackson.annotation.JsonFormat;

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
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDate startedAt,
            @JsonFormat(pattern = "yyyy.MM.dd")
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
            List<TimelineDateGroupDto> dateGroups
    ) {}

    public record TimelineItemDto(
            Long timelineId,
            TimelineType type,
            LocalDateTime occurredAt,
            String title,
            String subtitle,
            String previewText,
            Long targetId
    ) {}

    public record TimelineDateGroupDto(
            Integer year,
            String monthDay,
            boolean showYear,
            List<TimelineItemDto> items
    ) {}

    public record TimelineDetailDto(
            Long timelineId,
            TimelineType type,
            LocalDateTime occurredAt,
            TimelineDetail detail
    ) {}

    public sealed interface TimelineDetail
            permits TimelineRegisterDetailDto,
                    TimelineStatusDetailDto,
                    TimelineFocusDetailDto,
                    TimelineRecordDetailDto {
    }

    public record TimelineRegisterDetailDto(
            String description
    ) implements TimelineDetail {}

    public record TimelineStatusDetailDto(
            String title,
            String description
    ) implements TimelineDetail {}

    public record TimelineFocusDetailDto(
            String timeText,
            Integer page
    ) implements TimelineDetail {}

    public record TimelineRecordDetailDto(
            String content,
            String emotion,
            List<String> imageUrls
    ) implements TimelineDetail {}
}
