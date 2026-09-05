package app.nook.timeline.service;

import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.repository.RecordRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.converter.TimelineResponseConverter;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineQueryService {

    private static final DateTimeFormatter FOCUS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LibraryRepository libraryRepository;
    private final FocusRepository focusRepository;
    private final RecordRepository recordRepository;
    private final TimelineRepository timelineRepository;
    private final PresignedUrlService presignedUrlService;

    public TimelineResponseDto.TimelineSummaryDto getTimelineSummary(User user, Long libraryId) {
        Library library = getOwnedLibrary(user, libraryId);

        // 서재 요약은 포커스/기록 집계와 최신 타임라인 preview를 함께 반환한다.
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
                        Math.toIntExact(recordRepository.countByLibraryId(library.getId())),
                        getLatestRecordPreview(library.getId())
                );

        List<Timeline> previewTimelines = timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library);

        TimelineResponseDto.TimelinePreviewDto timelinePreview = TimelineResponseConverter.toTimelinePreview(
                toTimelineDateGroups(previewTimelines)
        );

        return TimelineResponseConverter.toTimelineSummary(
                library.getId(),
                focusSummary,
                recordSummary,
                timelinePreview
        );
    }

    public TimelineResponseDto.TimelinePreviewDto getTimelinePreview(User user, Long libraryId) {
        Library library = getOwnedLibrary(user, libraryId);
        // TODO: 전체보기 무한 스크롤이 필요하면 cursor 기반 조회를 도입한다.
        List<Timeline> timelines = timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library);
        return TimelineResponseConverter.toTimelinePreview(toTimelineDateGroups(timelines));
    }

    public TimelineResponseDto.TimelineDetailDto getTimelineDetail(User user, Long libraryId, Long timelineId) {
        Library library = getOwnedLibrary(user, libraryId);
        Timeline timeline = timelineRepository.findByIdAndLibrary(timelineId, library)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        return switch (timeline.getType()) {
            case REGISTER -> TimelineResponseConverter.toTimelineDetail(
                    timeline,
                    TimelineResponseConverter.toRegisterDetail("서재에 등록했어요.")
            );
            case STATUS -> TimelineResponseConverter.toTimelineDetail(
                    timeline,
                    TimelineResponseConverter.toStatusDetail(
                            toStatusTitle(timeline.getPreviewText()),
                            "독서 상태를 변경했어요."
                    )
            );
            case FOCUS -> toFocusTimelineDetail(timeline);
            case RECORD -> toRecordTimelineDetail(timeline, user.getId());
        };
    }

    private Library getOwnedLibrary(User user, Long libraryId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        if (!library.getUser().getId().equals(user.getId())) {
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        return library;
    }

    private List<TimelineResponseDto.TimelineDateGroupDto> toTimelineDateGroups(List<Timeline> timelines) {
        // FOCUS/RECORD 원본은 타입별로 미리 묶어서 조회해 item 조립 시 N+1을 피한다.
        Map<Long, Focus> focusMap = getFocusMap(timelines);
        Map<Long, Record> recordMap = getRecordMap(timelines);
        Map<LocalDate, List<TimelineResponseDto.TimelineItemDto>> grouped = new LinkedHashMap<>();

        for (Timeline timeline : timelines) {
            LocalDate date = timeline.getOccurredAt().toLocalDate();
            grouped.computeIfAbsent(date, ignored -> new ArrayList<>())
                    .add(toTimelineItem(timeline, focusMap, recordMap));
        }

        Integer previousYear = null;
        List<TimelineResponseDto.TimelineDateGroupDto> dateGroups = new ArrayList<>();

        for (Map.Entry<LocalDate, List<TimelineResponseDto.TimelineItemDto>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            // 같은 연도 그룹이 연속되면 첫 그룹만 연도를 노출한다.
            boolean showYear = previousYear == null || previousYear != date.getYear();
            dateGroups.add(TimelineResponseConverter.toTimelineDateGroup(date, showYear, entry.getValue()));
            previousYear = date.getYear();
        }

        return dateGroups;
    }

    private TimelineResponseDto.TimelineItemDto toTimelineItem(
            Timeline timeline,
            Map<Long, Focus> focusMap,
            Map<Long, Record> recordMap
    ) {
        return switch (timeline.getType()) {
            case REGISTER -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    timeline.getLibrary().getBook().getTitle(),
                    "서재에 등록했어요."
            );
            case FOCUS -> toFocusTimelineItem(timeline, focusMap);
            case RECORD -> toRecordTimelineItem(timeline, recordMap);
            case STATUS -> TimelineResponseConverter.toTimelineItem(
                    timeline,
                    toStatusTitle(timeline.getPreviewText()),
                    "독서 상태를 변경했어요."
            );
        };
    }

    private String toStatusTitle(String previewText) {
        if (previewText == null) {
            return "독서 상태 변경";
        }

        if (previewText.endsWith("READING")) {
            return "독서 중";
        }
        if (previewText.endsWith("FINISHED")) {
            return "완독";
        }
        if (previewText.endsWith("BEFORE")) {
            return "읽기 전";
        }

        return "독서 상태 변경";
    }

    private TimelineResponseDto.TimelineItemDto toFocusTimelineItem(Timeline timeline, Map<Long, Focus> focusMap) {
        return Optional.ofNullable(focusMap.get(timeline.getTargetId()))
                .filter(focus -> focus.getLibrary().getId().equals(timeline.getLibrary().getId()))
                .filter(focus -> focus.getEndedAt() != null)
                .map(focus -> TimelineResponseConverter.toTimelineItem(
                        timeline,
                        timeline.getPreviewText(),
                        toFocusSubtitle(focus)
                ))
                .orElseGet(() -> TimelineResponseConverter.toTimelineItem(
                        timeline,
                        timeline.getPreviewText() != null ? timeline.getPreviewText() : "포커스",
                        null
                ));
    }

    private Map<Long, Focus> getFocusMap(List<Timeline> timelines) {
        // 포커스 타임라인에 필요한 원본을 한 번에 조회해 subtitle 조립에 사용한다.
        List<Long> focusIds = timelines.stream()
                .filter(timeline -> timeline.getType() == TimelineType.FOCUS)
                .map(Timeline::getTargetId)
                .distinct()
                .toList();

        if (focusIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return focusRepository.findAllById(focusIds).stream()
                .collect(Collectors.toMap(Focus::getId, Function.identity()));
    }

    private Map<Long, Record> getRecordMap(List<Timeline> timelines) {
        // 기록 타임라인 preview 보강에 필요한 원본을 한 번에 조회한다.
        List<Long> recordIds = timelines.stream()
                .filter(timeline -> timeline.getType() == TimelineType.RECORD)
                .map(Timeline::getTargetId)
                .distinct()
                .toList();

        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return recordRepository.findAllById(recordIds).stream()
                .collect(Collectors.toMap(Record::getId, Function.identity()));
    }

    private String toFocusSubtitle(Focus focus) {
        return focus.getStartedAt().format(FOCUS_TIME_FORMATTER)
                + " - "
                + (focus.getEndedAt().equals(focus.getStartedAt().toLocalDate().plusDays(1).atStartOfDay())
                ? "24:00"
                : focus.getEndedAt().format(FOCUS_TIME_FORMATTER));
    }

    private TimelineResponseDto.TimelineItemDto toRecordTimelineItem(Timeline timeline, Map<Long, Record> recordMap) {
        Record record = recordMap.get(timeline.getTargetId());
        String previewText = timeline.getPreviewText();
        if ((previewText == null || previewText.isBlank()) && record != null
                && record.getLibrary().getId().equals(timeline.getLibrary().getId())) {
            previewText = toRecordPreviewText(record);
        }

        return new TimelineResponseDto.TimelineItemDto(
                timeline.getId(),
                timeline.getType(),
                timeline.getOccurredAt(),
                "독서 기록",
                null,
                previewText,
                timeline.getTargetId()
        );
    }

    private String getLatestRecordPreview(Long libraryId) {
        return recordRepository.findRecentByLibraryId(libraryId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(this::toRecordPreviewText)
                .orElse(null);
    }

    private String toRecordPreviewText(Record record) {
        String content = record.getContent();
        if (content != null) {
            String trimmed = content.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }

        int imageCount = record.getImages() != null ? record.getImages().size() : 0;
        if (imageCount > 0) {
            return imageCount + "개의 이미지";
        }

        return "독서 기록";
    }

    private TimelineResponseDto.TimelineDetailDto toFocusTimelineDetail(Timeline timeline) {
        // 원본 포커스가 없어도 타임라인 이벤트는 유지하고, 복원 가능한 정보만 fallback으로 노출한다.
        return focusRepository.findById(timeline.getTargetId())
                .filter(focus -> focus.getLibrary().getId().equals(timeline.getLibrary().getId()))
                .filter(focus -> focus.getEndedAt() != null)
                .map(focus -> TimelineResponseConverter.toTimelineDetail(
                        timeline,
                        TimelineResponseConverter.toFocusDetail(
                                toFocusSubtitle(focus) + " (" + toFocusDurationText(focus.getDurationSec()) + ")",
                                focus.getEndPage()
                        )
                ))
                .orElseGet(() -> {
                    log.warn("[TIMELINE_DETAIL_FOCUS_MISSING] timelineId={}, targetId={}",
                            timeline.getId(), timeline.getTargetId());
                    return TimelineResponseConverter.toTimelineDetail(
                            timeline,
                            TimelineResponseConverter.toFocusDetail(timeline.getPreviewText(), null)
                    );
                });
    }

    private TimelineResponseDto.TimelineDetailDto toRecordTimelineDetail(Timeline timeline, Long userId) {
        // 원본 기록이 없어도 저장된 previewText를 이용해 타임라인 상세를 최대한 유지한다.
        return recordRepository.findWithImagesById(timeline.getTargetId())
                .filter(record -> record.getLibrary().getId().equals(timeline.getLibrary().getId()))
                .map(record -> TimelineResponseConverter.toTimelineDetail(
                        timeline,
                        TimelineResponseConverter.toRecordDetail(
                                record.getContent(),
                                record.getEmotion() != null ? record.getEmotion().name() : null,
                                toRecordImageUrls(record, userId)
                        )
                ))
                .orElseGet(() -> {
                    log.warn("[TIMELINE_DETAIL_RECORD_MISSING] timelineId={}, targetId={}",
                            timeline.getId(), timeline.getTargetId());
                    return TimelineResponseConverter.toTimelineDetail(
                            timeline,
                            TimelineResponseConverter.toRecordDetail(
                                    timeline.getPreviewText(),
                                    null,
                                    List.of()
                            )
                    );
                });
    }

    private List<String> toRecordImageUrls(Record record, Long userId) {
        List<String> imageUrls = new ArrayList<>();

        for (RecordImage image : record.getImages()) {
            String key = image.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            try {
                imageUrls.add(presignedUrlService.getImageUrl(userId, key));
            } catch (RuntimeException ex) {
                log.warn("[TIMELINE_DETAIL_RECORD_IMAGE_URL_FAILED] recordId={}, key={}",
                        record.getId(), key, ex);
            }
        }

        return imageUrls;
    }

    private String toFocusDurationText(Integer durationSec) {
        if (durationSec == null || durationSec <= 0) {
            return "0분";
        }
        if (durationSec < 60) {
            return "1분 미만";
        }

        int hour = durationSec / 3600;
        int minute = (durationSec % 3600) / 60;

        if (hour > 0 && minute > 0) {
            return hour + "시간 " + minute + "분";
        }
        if (hour > 0) {
            return hour + "시간";
        }
        return minute + "분";
    }
}
