package app.nook.timeline.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.repository.RecordRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimelineQueryService 테스트")
class TimelineQueryServiceTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private TimelineRepository timelineRepository;

    @InjectMocks
    private TimelineQueryService timelineQueryService;

    private User user(Long id) {
        User user = User.builder()
                .email("timeline@test.com")
                .nickName("timeline-user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-" + id)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Library library(User user, Long libraryId) {
        Book book = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        ReflectionTestUtils.setField(book, "id", 101L);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", libraryId);
        ReflectionTestUtils.setField(library, "startedAt", LocalDate.of(2025, 12, 30));
        ReflectionTestUtils.setField(library, "endedAt", LocalDate.of(2026, 1, 19));
        ReflectionTestUtils.setField(library, "focusSec", 12262L);
        ReflectionTestUtils.setField(library, "page", 99);
        return library;
    }

    private Focus focus(Long focusId, Library library, LocalDateTime startedAt, LocalDateTime endedAt, int durationSec) {
        Theme theme = Theme.builder().build();

        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(durationSec)
                .build();
        ReflectionTestUtils.setField(focus, "id", focusId);
        return focus;
    }

    private Timeline timeline(
            Long timelineId,
            Library library,
            TimelineType type,
            LocalDateTime occurredAt,
            String previewText,
            Long targetId
    ) {
        Timeline timeline = Timeline.builder()
                .library(library)
                .type(type)
                .targetId(targetId)
                .occurredAt(occurredAt)
                .previewText(previewText)
                .build();
        ReflectionTestUtils.setField(timeline, "id", timelineId);
        return timeline;
    }

    private Record record(Long recordId, Library library, String content) {
        Record record = Record.create(library, null, content);
        ReflectionTestUtils.setField(record, "id", recordId);
        return record;
    }

    private Record recordWithImages(Long recordId, Library library, String content, String emotion, List<String> keys) {
        Record record = Record.create(library, null, content);
        ReflectionTestUtils.setField(record, "id", recordId);
        if (emotion != null) {
            ReflectionTestUtils.setField(record, "emotion", Emotion.valueOf(emotion));
        }

        List<RecordImage> images = keys.stream()
                .map(key -> RecordImage.builder()
                        .record(record)
                        .key(key)
                        .orderIndex(0)
                        .build())
                .toList();
        ReflectionTestUtils.setField(record, "images", images);
        return record;
    }

    @Nested
    @DisplayName("독서 이력 요약 조회")
    class GetTimelineSummary {

        @Test
        @DisplayName("성공")
        void getTimelineSummary_성공() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(10L, library, TimelineType.REGISTER,
                            LocalDateTime.of(2026, 1, 12, 21, 10), "서재에 등록했어요", 12L),
                    timeline(9L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 11, 20, 0), "기록 preview", 9001L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(focusRepository.countByLibrary(library)).willReturn(39);
            given(recordRepository.countByLibraryId(12L)).willReturn(1L);
            given(recordRepository.findRecentByLibraryId(eq(12L), any())).willReturn(List.of(record(9001L, library, "기록 preview")));
            given(timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);

            TimelineResponseDto.TimelineSummaryDto result =
                    timelineQueryService.getTimelineSummary(user, 12L);

            assertThat(result.libraryId()).isEqualTo(12L);
            assertThat(result.focusSummary().totalFocusSec()).isEqualTo(12262L);
            assertThat(result.focusSummary().focusCount()).isEqualTo(39);
            assertThat(result.recordSummary().recordCount()).isEqualTo(1);
            assertThat(result.recordSummary().latestRecordPreview()).isEqualTo("기록 preview");
            assertThat(result.timelinePreview().dateGroups()).hasSize(2);
        }

        @Test
        @DisplayName("최신 기록이 이미지 전용이면 이미지 개수 preview를 반환한다")
        void getTimelineSummary_latestRecordPreview_이미지전용기록() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(10L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 12, 21, 10), "3개의 이미지", 9001L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(focusRepository.countByLibrary(library)).willReturn(39);
            given(recordRepository.countByLibraryId(12L)).willReturn(1L);
            given(recordRepository.findRecentByLibraryId(eq(12L), any())).willReturn(List.of(
                    recordWithImages(9001L, library, "   ", null, List.of("a.png", "b.png", "c.png"))
            ));
            given(timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);

            TimelineResponseDto.TimelineSummaryDto result =
                    timelineQueryService.getTimelineSummary(user, 12L);

            assertThat(result.recordSummary().latestRecordPreview()).isEqualTo("3개의 이미지");
        }

        @Test
        @DisplayName("summary preview는 최신 5건만 사용한다")
        void getTimelineSummary_preview_5건제한() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(15L, library, TimelineType.RECORD, LocalDateTime.of(2026, 1, 15, 12, 0), "1", 1L),
                    timeline(14L, library, TimelineType.RECORD, LocalDateTime.of(2026, 1, 14, 12, 0), "2", 2L),
                    timeline(13L, library, TimelineType.RECORD, LocalDateTime.of(2026, 1, 13, 12, 0), "3", 3L),
                    timeline(12L, library, TimelineType.RECORD, LocalDateTime.of(2026, 1, 12, 12, 0), "4", 4L),
                    timeline(11L, library, TimelineType.RECORD, LocalDateTime.of(2026, 1, 11, 12, 0), "5", 5L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(focusRepository.countByLibrary(library)).willReturn(39);
            given(recordRepository.countByLibraryId(12L)).willReturn(5L);
            given(recordRepository.findRecentByLibraryId(eq(12L), any())).willReturn(List.of(record(6L, library, "1")));
            given(timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);

            TimelineResponseDto.TimelineSummaryDto result =
                    timelineQueryService.getTimelineSummary(user, 12L);

            int itemCount = result.timelinePreview().dateGroups().stream()
                    .mapToInt(group -> group.items().size())
                    .sum();

            assertThat(itemCount).isEqualTo(5);
        }

        @Test
        @DisplayName("서재가 없으면 예외를 던진다")
        void getTimelineSummary_서재없음_예외() {
            User user = user(1L);
            given(libraryRepository.findById(12L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> timelineQueryService.getTimelineSummary(user, 12L)
            );

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("다른 사용자의 서재면 예외를 던진다")
        void getTimelineSummary_권한없음_예외() {
            User owner = user(2L);
            User requester = user(1L);
            Library library = library(owner, 12L);
            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> timelineQueryService.getTimelineSummary(requester, 12L)
            );

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("독서 이력 preview 조회")
    class GetTimelinePreview {

        @Test
        @DisplayName("기록이 없으면 빈 그룹 응답을 반환한다")
        void getTimelinePreview_빈응답() {
            User user = user(1L);
            Library library = library(user, 12L);

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(List.of());

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            assertThat(result.dateGroups()).isEmpty();
        }

        @Test
        @DisplayName("같은 날짜의 item은 하나의 그룹으로 묶인다")
        void getTimelinePreview_같은날짜_그룹핑() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(20L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 12, 21, 10), "기록 preview", 9001L),
                    timeline(19L, library, TimelineType.FOCUS,
                            LocalDateTime.of(2026, 1, 12, 16, 0), "54분 포커스", 7001L),
                    timeline(18L, library, TimelineType.REGISTER,
                            LocalDateTime.of(2026, 1, 11, 12, 0), "서재에 등록했어요", 12L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);
            given(focusRepository.findAllById(List.of(7001L))).willReturn(Collections.emptyList());
            given(recordRepository.findAllById(List.of(9001L))).willReturn(List.of(record(9001L, library, "기록 preview")));

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            assertThat(result.dateGroups()).hasSize(2);
            assertThat(result.dateGroups().get(0).monthDay()).isEqualTo("01.12");
            assertThat(result.dateGroups().get(0).items()).hasSize(2);
        }

        @Test
        @DisplayName("연도가 바뀌는 그룹만 showYear가 true다")
        void getTimelinePreview_showYear_그룹기준() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(20L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 12, 21, 10), "기록 preview", 9001L),
                    timeline(19L, library, TimelineType.FOCUS,
                            LocalDateTime.of(2026, 1, 11, 16, 0), "54분 포커스", 7001L),
                    timeline(18L, library, TimelineType.REGISTER,
                            LocalDateTime.of(2025, 12, 31, 12, 0), "서재에 등록했어요", 12L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);
            given(focusRepository.findAllById(List.of(7001L))).willReturn(Collections.emptyList());
            given(recordRepository.findAllById(List.of(9001L))).willReturn(List.of(record(9001L, library, "기록 preview")));

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            assertThat(result.dateGroups()).hasSize(3);
            assertThat(result.dateGroups().get(0).showYear()).isTrue();
            assertThat(result.dateGroups().get(1).showYear()).isFalse();
            assertThat(result.dateGroups().get(2).showYear()).isTrue();
        }

        @Test
        @DisplayName("타입별 title을 매핑한다")
        void getTimelinePreview_타입별_매핑() {
            User user = user(1L);
            Library library = library(user, 12L);
            Focus focus = focus(
                    7001L,
                    library,
                    LocalDateTime.of(2026, 1, 10, 16, 0),
                    LocalDateTime.of(2026, 1, 10, 16, 54),
                    3240
            );
            ReflectionTestUtils.setField(focus, "endPage", 72);

            List<Timeline> timelines = List.of(
                    timeline(20L, library, TimelineType.REGISTER,
                            LocalDateTime.of(2026, 1, 12, 21, 10), "서재에 등록했어요", 12L),
                    timeline(19L, library, TimelineType.STATUS,
                            LocalDateTime.of(2026, 1, 11, 21, 10), "독서 상태 변경: READING", 12L),
                    timeline(18L, library, TimelineType.FOCUS,
                            LocalDateTime.of(2026, 1, 10, 16, 54), "54분의 포커스", 7001L),
                    timeline(17L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 9, 21, 10), "기록 preview", 9001L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);
            given(focusRepository.findAllById(List.of(7001L))).willReturn(List.of(focus));
            given(recordRepository.findAllById(List.of(9001L))).willReturn(List.of(record(9001L, library, "기록 preview")));

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            List<TimelineResponseDto.TimelineItemDto> items = result.dateGroups().stream()
                    .flatMap(group -> group.items().stream())
                    .toList();

            assertThat(items.get(0).title()).isEqualTo(library.getBook().getTitle());
            assertThat(items.get(1).title()).isEqualTo("독서 중");
            assertThat(items.get(1).subtitle()).isEqualTo("독서 상태를 변경했어요.");
            assertThat(items.get(2).title()).isEqualTo("54분의 포커스");
            assertThat(items.get(2).subtitle()).isEqualTo("16:00 - 16:54");
            assertThat(items.get(3).title()).isEqualTo("독서 기록");
            assertThat(items.get(0).subtitle()).isEqualTo("서재에 등록했어요.");
        }

        @Test
        @DisplayName("다음 날 자정에 끝난 세그먼트는 24시로 표시한다")
        void getTimelinePreview_다음날자정_24시표시() {
            User user = user(1L);
            Library library = library(user, 12L);
            Focus focus = focus(
                    7001L,
                    library,
                    LocalDateTime.of(2026, 1, 10, 23, 0),
                    LocalDateTime.of(2026, 1, 11, 0, 0),
                    3600
            );
            Timeline timeline = timeline(
                    18L,
                    library,
                    TimelineType.FOCUS,
                    LocalDateTime.of(2026, 1, 10, 23, 0),
                    "1시간의 포커스",
                    7001L
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library)).willReturn(List.of(timeline));
            given(focusRepository.findAllById(List.of(7001L))).willReturn(List.of(focus));

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            assertThat(result.dateGroups().get(0).items().get(0).subtitle()).isEqualTo("23:00 - 24:00");
        }

        @Test
        @DisplayName("record preview가 비어 있으면 기록 규칙으로 fallback 한다")
        void getTimelinePreview_recordPreviewFallback() {
            User user = user(1L);
            Library library = library(user, 12L);

            List<Timeline> timelines = List.of(
                    timeline(17L, library, TimelineType.RECORD,
                            LocalDateTime.of(2026, 1, 9, 21, 10), "   ", 9001L)
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library))
                    .willReturn(timelines);
            given(recordRepository.findAllById(List.of(9001L))).willReturn(List.of(
                    recordWithImages(9001L, library, "   ", null, List.of("a.png", "b.png", "c.png"))
            ));

            TimelineResponseDto.TimelinePreviewDto result =
                    timelineQueryService.getTimelinePreview(user, 12L);

            TimelineResponseDto.TimelineItemDto item = result.dateGroups().get(0).items().get(0);
            assertThat(item.title()).isEqualTo("독서 기록");
            assertThat(item.previewText()).isEqualTo("3개의 이미지");
        }

        @Test
        @DisplayName("preview 조회 시 서재를 확인한다")
        void getTimelinePreview_서재확인() {
            User user = user(1L);
            given(libraryRepository.findById(12L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> timelineQueryService.getTimelinePreview(user, 12L)
            );

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("독서 이력 상세 조회")
    class GetTimelineDetail {

        @Test
        @DisplayName("FOCUS 상세 조회 성공")
        void getTimelineDetail_focus_성공() {
            User user = user(1L);
            Library library = library(user, 12L);
            Timeline timeline = timeline(
                    30L,
                    library,
                    TimelineType.FOCUS,
                    LocalDateTime.of(2026, 1, 10, 16, 54),
                    "54분의 포커스",
                    7001L
            );
            Focus focus = focus(
                    7001L,
                    library,
                    LocalDateTime.of(2026, 1, 10, 16, 0),
                    LocalDateTime.of(2026, 1, 10, 16, 54),
                    3240
            );
            ReflectionTestUtils.setField(focus, "endPage", 72);

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByIdAndLibrary(30L, library)).willReturn(Optional.of(timeline));
            given(focusRepository.findById(7001L)).willReturn(Optional.of(focus));

            TimelineResponseDto.TimelineDetailDto result =
                    timelineQueryService.getTimelineDetail(user, 12L, 30L);

            assertThat(result.type()).isEqualTo(TimelineType.FOCUS);
            assertThat(result.detail()).isInstanceOf(TimelineResponseDto.TimelineFocusDetailDto.class);
            TimelineResponseDto.TimelineFocusDetailDto detail =
                    (TimelineResponseDto.TimelineFocusDetailDto) result.detail();
            assertThat(detail.timeText()).isEqualTo("16:00 - 16:54 (54분)");
            assertThat(detail.page()).isEqualTo(72);
        }

        @Test
        @DisplayName("RECORD 상세 조회 성공")
        void getTimelineDetail_record_성공() {
            User user = user(1L);
            Library library = library(user, 12L);
            Timeline timeline = timeline(
                    31L,
                    library,
                    TimelineType.RECORD,
                    LocalDateTime.of(2026, 1, 12, 21, 10),
                    "기록 preview",
                    9001L
            );
            Record record = recordWithImages(
                    9001L,
                    library,
                    "말하기와 듣기...",
                    "FUN",
                    List.of("record/users/1/a.png", "record/users/1/b.png")
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByIdAndLibrary(31L, library)).willReturn(Optional.of(timeline));
            given(recordRepository.findWithImagesById(9001L)).willReturn(Optional.of(record));
            given(presignedUrlService.getImageUrl(1L, "record/users/1/a.png")).willReturn("https://img/a");
            given(presignedUrlService.getImageUrl(1L, "record/users/1/b.png")).willReturn("https://img/b");

            TimelineResponseDto.TimelineDetailDto result =
                    timelineQueryService.getTimelineDetail(user, 12L, 31L);

            assertThat(result.type()).isEqualTo(TimelineType.RECORD);
            assertThat(result.detail()).isInstanceOf(TimelineResponseDto.TimelineRecordDetailDto.class);
            TimelineResponseDto.TimelineRecordDetailDto detail =
                    (TimelineResponseDto.TimelineRecordDetailDto) result.detail();
            assertThat(detail.content()).isEqualTo("말하기와 듣기...");
            assertThat(detail.emotion()).isEqualTo("FUN");
            assertThat(detail.imageUrls()).containsExactly("https://img/a", "https://img/b");
        }

        @Test
        @DisplayName("RECORD 상세 조회 시 이미지 URL 생성 하나가 실패해도 나머지는 반환한다")
        void getTimelineDetail_record_이미지일부실패_성공() {
            User user = user(1L);
            Library library = library(user, 12L);
            Timeline timeline = timeline(
                    31L,
                    library,
                    TimelineType.RECORD,
                    LocalDateTime.of(2026, 1, 12, 21, 10),
                    "기록 preview",
                    9001L
            );
            Record record = recordWithImages(
                    9001L,
                    library,
                    "말하기와 듣기...",
                    "FUN",
                    List.of("record/users/1/a.png", "record/users/1/b.png")
            );

            given(libraryRepository.findById(12L)).willReturn(Optional.of(library));
            given(timelineRepository.findByIdAndLibrary(31L, library)).willReturn(Optional.of(timeline));
            given(recordRepository.findWithImagesById(9001L)).willReturn(Optional.of(record));
            given(presignedUrlService.getImageUrl(1L, "record/users/1/a.png")).willReturn("https://img/a");
            willThrow(new RuntimeException("presigned url failed"))
                    .given(presignedUrlService).getImageUrl(1L, "record/users/1/b.png");

            TimelineResponseDto.TimelineDetailDto result =
                    timelineQueryService.getTimelineDetail(user, 12L, 31L);

            TimelineResponseDto.TimelineRecordDetailDto detail =
                    (TimelineResponseDto.TimelineRecordDetailDto) result.detail();
            assertThat(detail.imageUrls()).containsExactly("https://img/a");
        }
    }
}
