package app.nook.focus.service;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.ThemeRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.FocusFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.ThemeFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FocusService 테스트")
class FocusServiceTest {

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private TimelineCommandService timelineCommandService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FocusService focusService;

    private User user;
    private Library library;
    private Theme theme;
    private Focus focus;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        Book book = FocusFixture.book();
        library = LibraryFixture.library(user, book);
        theme = ThemeFixture.theme();
        focus = FocusFixture.focus(library, theme);
    }

    @Nested
    @DisplayName("포커스 시작")
    class StartFocus {

        @Test
        @DisplayName("성공")
        void 성공() {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.empty());
            given(libraryRepository.findByIdAndUserId(library.getId(), user.getId()))
                    .willReturn(Optional.of(library));
            given(themeRepository.findById(theme.getId()))
                    .willReturn(Optional.of(theme));
            given(focusRepository.save(any(Focus.class)))
                    .willAnswer(invocation -> {
                        Focus saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 100L);
                        return saved;
                    });

            FocusResponseDto.FocusStart result = focusService.startFocus(user, request);

            assertThat(result).isNotNull();
            assertThat(result.focusId()).isEqualTo(100L);
            assertThat(result.libraryId()).isEqualTo(library.getId());
            assertThat(result.bookId()).isEqualTo(library.getBook().getId());
            assertThat(result.bookTitle()).isEqualTo("첫사랑의 침공");
            assertThat(result.author()).isEqualTo("권혁일");
            assertThat(result.themeId()).isEqualTo(theme.getId());
            assertThat(result.themeName()).isEqualTo("THEME1");
            assertThat(result.startedAt()).isNotNull();
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
        }

        @Test
        @DisplayName("실패 - 이미 진행 중인 포커스가 있음")
        void 실패_이미진행중() {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.of(focus));

            assertThatThrownBy(() -> focusService.startFocus(user, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
        }

        @Test
        @DisplayName("실패 - 서재를 찾을 수 없음")
        void 실패_서재없음() {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.empty());
            given(libraryRepository.findByIdAndUserId(library.getId(), user.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> focusService.startFocus(user, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.LIBRARY_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 테마가 없음")
        void 실패_테마없음() {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.empty());
            given(libraryRepository.findByIdAndUserId(library.getId(), user.getId()))
                    .willReturn(Optional.of(library));
            given(themeRepository.findById(theme.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> focusService.startFocus(user, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.THEME_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("포커스 종료")
    class EndFocus {

        @Test
        @DisplayName("성공 - 완독 처리")
        void 성공_완독() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, true);

            given(focusRepository.findByIdAndLibraryUserId(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(result).isNotNull();
            assertThat(result.focusId()).isEqualTo(focus.getId());
            assertThat(result.libraryId()).isEqualTo(library.getId());
            assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 22, 14, 0, 0));
            assertThat(result.endedAt()).isNotNull();
            assertThat(result.durationSec()).isGreaterThanOrEqualTo(0);
            assertThat(result.page()).isEqualTo(72);
            assertThat(focus.getEndPage()).isEqualTo(72);
            assertThat(result.totalFocusSec()).isGreaterThanOrEqualTo(0L);
            assertThat(result.readingStatus()).isEqualTo("FINISHED");
            verify(timelineCommandService).appendFocusCompleted(focus);
            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().isEmpty()
            ));
        }

        @Test
        @DisplayName("성공 - 완독하지 않으면 READING 유지")
        void 성공_독서중유지() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

            given(focusRepository.findByIdAndLibraryUserId(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(result).isNotNull();
            assertThat(result.page()).isEqualTo(45);
            assertThat(focus.getEndPage()).isEqualTo(45);
            assertThat(result.readingStatus()).isEqualTo("READING");
            verify(timelineCommandService).appendFocusCompleted(focus);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패 - 포커스를 찾을 수 없음")
        void 실패_포커스없음() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, false);

            given(focusRepository.findByIdAndLibraryUserId(focus.getId(), user.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 이미 종료된 포커스")
        void 실패_이미종료됨() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, false);
            ReflectionTestUtils.setField(focus, "endedAt", LocalDateTime.of(2026, 3, 22, 14, 34, 26));

            given(focusRepository.findByIdAndLibraryUserId(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_ENDED);
        }
    }
}
