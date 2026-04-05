package app.nook.timeline.converter;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;

import java.time.LocalDateTime;


public class TimelineConverter {

    public static Timeline toTimeline(
            Library library,
            TimelineType timelineType,
            Long targetId,
            LocalDateTime occurredAt,
            String previewText
    ) {
        return new Timeline(
                library,
                timelineType,
                targetId,
                occurredAt,
                previewText
        );
    }
}
