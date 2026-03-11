package app.nook.timeline.service;

import app.nook.library.domain.Library;
import app.nook.timeline.converter.TimeLineConverter;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;
import app.nook.timeline.repository.BookTimeLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineCommandService {

    private static final String REGISTER_PREVIEW = "서재에 등록했어요";
    private static final String STATUS_PREVIEW_PREFIX = "독서 상태 변경: ";

    private final BookTimeLineRepository bookTimeLineRepository;

    // TODO: 추후 기능 추가 예정

    @Transactional
    public void appendRegister(Library library) {
        BookTimeLine timeline = TimeLineConverter.toBookTimeLine(
                library,
                BookTimeLineType.REGISTER,
                library.getId(),
                library.getCreatedDate(),
                REGISTER_PREVIEW
        );
        bookTimeLineRepository.save(timeline);
    }

    @Transactional
    public void appendStatusChanged(Library library) {
        BookTimeLine timeline = TimeLineConverter.toBookTimeLine(
                library,
                BookTimeLineType.STATUS,
                library.getId(),
                library.getModifiedDate(),
                STATUS_PREVIEW_PREFIX + library.getReadingStatus().name()
        );
        bookTimeLineRepository.save(timeline);
    }
}
