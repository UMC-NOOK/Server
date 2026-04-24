package app.nook.timeline.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.record.domain.Record;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimelineCommandService 테스트")
class TimelineCommandServiceTest {

    @Mock
    private TimelineRepository timelineRepository;

    @InjectMocks
    private TimelineCommandService timelineCommandService;

    @Captor
    private ArgumentCaptor<Timeline> timelineCaptor;

    private Library library;

    @BeforeEach
    void setUp() {
        library = library(12L, "첫사랑의 침공");
    }

    @Nested
    @DisplayName("서재 등록 타임라인 추가")
    class AppendRegister {

        @Test
        @DisplayName("성공 - REGISTER 타임라인을 저장한다")
        void appendRegister_성공() {
            LocalDateTime createdDate = LocalDateTime.of(2025, 12, 30, 12, 0);
            ReflectionTestUtils.setField(library, "createdDate", createdDate);

            timelineCommandService.appendRegister(library);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getLibrary()).isEqualTo(library);
            assertThat(saved.getType()).isEqualTo(TimelineType.REGISTER);
            assertThat(saved.getTargetId()).isEqualTo(12L);
            assertThat(saved.getOccurredAt()).isEqualTo(createdDate);
            assertThat(saved.getPreviewText()).isEqualTo("서재에 등록했어요");
        }
    }

    @Nested
    @DisplayName("독서 상태 변경 타임라인 추가")
    class AppendStatusChanged {

        @Test
        @DisplayName("성공 - STATUS 타임라인을 저장한다")
        void appendStatusChanged_성공() {
            LocalDateTime modifiedDate = LocalDateTime.of(2026, 1, 1, 9, 0);
            LocalDateTime occurredAt = LocalDateTime.of(2026, 1, 2, 10, 0);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library, "modifiedDate", modifiedDate);

            timelineCommandService.appendStatusChanged(library, occurredAt);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(TimelineType.STATUS);
            assertThat(saved.getTargetId()).isEqualTo(12L);
            assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(saved.getOccurredAt()).isNotEqualTo(modifiedDate);
            assertThat(saved.getPreviewText()).isEqualTo("독서 상태 변경: READING");
        }
    }

    @Nested
    @DisplayName("포커스 종료 타임라인 추가")
    class AppendFocusCompleted {

        @Test
        @DisplayName("성공 - 분 단위 포커스 preview를 저장한다")
        void appendFocusCompleted_분단위() {
            Focus focus = focus(7001L, library, 3240, LocalDateTime.of(2026, 1, 10, 16, 54));

            timelineCommandService.appendFocusCompleted(focus);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(TimelineType.FOCUS);
            assertThat(saved.getTargetId()).isEqualTo(7001L);
            assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 1, 10, 16, 54));
            assertThat(saved.getPreviewText()).isEqualTo("54분의 포커스");
        }

        @Test
        @DisplayName("성공 - 시간과 분이 함께 있는 포커스 preview를 저장한다")
        void appendFocusCompleted_시간분() {
            Focus focus = focus(7002L, library, 4380, LocalDateTime.of(2026, 1, 10, 23, 15));

            timelineCommandService.appendFocusCompleted(focus);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getPreviewText()).isEqualTo("1시간 13분의 포커스");
        }

        @Test
        @DisplayName("성공 - 0초 포커스도 preview를 저장한다")
        void appendFocusCompleted_영초() {
            Focus focus = focus(7003L, library, 0, LocalDateTime.of(2026, 1, 10, 10, 0));

            timelineCommandService.appendFocusCompleted(focus);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getPreviewText()).isEqualTo("0분의 포커스");
        }
    }

    @Nested
    @DisplayName("기록 생성 타임라인 추가")
    class AppendRecordCreated {

        @Test
        @DisplayName("성공 - 본문이 있으면 trim한 본문을 preview로 저장한다")
        void appendRecordCreated_본문우선() {
            Record record = record(9001L, library, "  말하기와 듣기...  ", LocalDateTime.of(2026, 1, 12, 21, 10));

            timelineCommandService.appendRecordCreated(record, 2);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(TimelineType.RECORD);
            assertThat(saved.getTargetId()).isEqualTo(9001L);
            assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 1, 12, 21, 10));
            assertThat(saved.getPreviewText()).isEqualTo("말하기와 듣기...");
        }

        @Test
        @DisplayName("성공 - 본문이 없고 이미지가 있으면 이미지 개수를 preview로 저장한다")
        void appendRecordCreated_이미지Fallback() {
            Record record = record(9002L, library, "   ", LocalDateTime.of(2026, 1, 12, 21, 11));

            timelineCommandService.appendRecordCreated(record, 3);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getPreviewText()).isEqualTo("3개의 이미지");
        }

        @Test
        @DisplayName("성공 - 본문과 이미지가 모두 없으면 기본 preview를 저장한다")
        void appendRecordCreated_기본문구() {
            Record record = record(9003L, library, null, LocalDateTime.of(2026, 1, 12, 21, 12));

            timelineCommandService.appendRecordCreated(record, 0);

            verify(timelineRepository).save(timelineCaptor.capture());

            Timeline saved = timelineCaptor.getValue();
            assertThat(saved.getPreviewText()).isEqualTo("독서 기록");
        }
    }

    private Library library(Long id, String title) {
        User user = User.builder()
                .email("test@test.com")
                .nickName("tester")
                .role(UserRole.USER)
                .build();

        Book book = Book.builder()
                .title(title)
                .author("작가")
                .isbn13("1234567890123")
                .sourceType(SourceType.ALADIN)
                .build();

        Library library = new Library(user, book);
        ReflectionTestUtils.setField(library, "id", id);
        ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.BEFORE);
        ReflectionTestUtils.setField(library, "startedAt", LocalDate.of(2025, 12, 30));
        return library;
    }

    private Focus focus(Long id, Library library, int durationSec, LocalDateTime endedAt) {
        Theme theme = Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("theme.png")
                .build();

        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(endedAt.minusSeconds(durationSec))
                .endedAt(endedAt)
                .durationSec(durationSec)
                .build();

        ReflectionTestUtils.setField(focus, "id", id);
        return focus;
    }

    private Record record(Long id, Library library, String content, LocalDateTime createdDate) {
        Record record = Record.create(library, null, content);

        ReflectionTestUtils.setField(record, "id", id);
        ReflectionTestUtils.setField(record, "createdDate", createdDate);
        return record;
    }
}
