package app.nook.timeline.converter;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;

import java.time.LocalDateTime;


public class TimeLineConverter {

    public static BookTimeLine toBookTimeLine(
            Library library,
            BookTimeLineType bookTimeLineType,
            Long targetId,
            LocalDateTime occurredAt,
            String previewText
    ) {
        return new BookTimeLine(
                library,
                bookTimeLineType,
                targetId,
                occurredAt,
                previewText
        );
    }
}
