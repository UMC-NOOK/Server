package app.nook.timeline.converter;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;


public class TimeLineConverter {


    public static BookTimeLine toBookTimeLine(
            Library library,
            BookTimeLineType bookTimeLineType,
            String snapshotValue,
            Long targetId
    ) {
        return BookTimeLine.builder()
                .library(library)
                .type(BookTimeLineType.REGISTER)
                .snapshotValue(snapshotValue)
                .targetId(library.getId())
                .build();
    }
}
