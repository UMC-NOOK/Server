package app.nook.timeline.service;

import app.nook.focus.domain.Focus;
import app.nook.library.domain.Library;
import app.nook.record.domain.Record;
import app.nook.timeline.converter.TimelineConverter;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.repository.TimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineCommandService {

    private static final String REGISTER_PREVIEW = "서재에 등록했어요";
    private static final String STATUS_PREVIEW_PREFIX = "독서 상태 변경: ";
    private static final String FOCUS_PREVIEW_SUFFIX = "의 포커스";

    private final TimelineRepository timelineRepository;

    @Transactional
    public void appendRegister(Library library) {
        Timeline timeline = TimelineConverter.toTimeline(
                library,
                TimelineType.REGISTER,
                library.getId(),
                library.getCreatedDate(),
                REGISTER_PREVIEW
        );
        timelineRepository.save(timeline);
    }

    @Transactional
    public void appendStatusChanged(Library library, LocalDateTime occurredAt) {
        Timeline timeline = TimelineConverter.toTimeline(
                library,
                TimelineType.STATUS,
                library.getId(),
                occurredAt,
                STATUS_PREVIEW_PREFIX + library.getReadingStatus().name()
        );
        timelineRepository.save(timeline);
    }

    @Transactional
    public void appendFocusCompleted(Focus focus) {
        Timeline timeline = TimelineConverter.toTimeline(
                focus.getLibrary(),
                TimelineType.FOCUS,
                focus.getId(),
                focus.getEndedAt(),
                toFocusPreviewText(focus.getDurationSec())
        );
        timelineRepository.save(timeline);
    }

    @Transactional
    public void appendRecordCreated(Record record, int imageCount) {
        Timeline timeline = TimelineConverter.toTimeline(
                record.getLibrary(),
                TimelineType.RECORD,
                record.getId(),
                record.getCreatedDate(),
                toRecordPreviewText(record, imageCount)
        );
        timelineRepository.save(timeline);
    }

    private String toFocusPreviewText(Integer durationSec) {
        // durationSec을 '54분의 포커스', '1시간 13분의 포커스' 형식으로 변환한다.
        if (durationSec == null || durationSec <= 0) {
            return "0분" + FOCUS_PREVIEW_SUFFIX;
        }
        if (durationSec < 60) {
            return "1분 미만" + FOCUS_PREVIEW_SUFFIX;
        }

        int hour = durationSec / 3600;
        int minute = (durationSec % 3600) / 60;

        if (hour > 0 && minute > 0) {
            return hour + "시간 " + minute + "분" + FOCUS_PREVIEW_SUFFIX;
        }
        if (hour > 0) {
            return hour + "시간" + FOCUS_PREVIEW_SUFFIX;
        }
        return minute + "분" + FOCUS_PREVIEW_SUFFIX;
    }

    private String toRecordPreviewText(Record record, int imageCount) {
        // 기록 preview는 본문 우선, 본문이 없으면 이미지 개수 문구를 사용한다.
        String content = record.getContent();
        if (content != null) {
            String trimmed = content.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }

        if (imageCount > 0) {
            return imageCount + "개의 이미지";
        }

        return "독서 기록";
    }
}
