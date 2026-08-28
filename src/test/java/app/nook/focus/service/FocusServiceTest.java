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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    private FocusService focusService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 3, 22, 14, 34, 26);

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
        useClock(DEFAULT_NOW);
    }

    private void useClock(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(KST).toInstant(), KST);
        focusService = new FocusService(
                focusRepository,
                libraryRepository,
                themeRepository,
                timelineCommandService,
                eventPublisher,
                new FocusDailyTimeCalculator(),
                clock
        );
    }

    private void setFocusStartedAt(LocalDateTime startedAt) {
        ReflectionTestUtils.setField(focus, "startedAt", startedAt);
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
            assertThat(result.startedAt()).isEqualTo(DEFAULT_NOW);
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
        }

        @Test
        @DisplayName("성공 - 저장 시작 시각을 초 단위로 정규화한다")
        void 성공_시작시각초단위정규화() {
            LocalDateTime serverNow = LocalDateTime.of(2026, 8, 1, 23, 55, 12, 987_654_321);
            LocalDateTime normalizedNow = LocalDateTime.of(2026, 8, 1, 23, 55, 12);
            useClock(serverNow);
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());
            ArgumentCaptor<Focus> focusCaptor = ArgumentCaptor.forClass(Focus.class);

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.empty());
            given(libraryRepository.findByIdAndUserId(library.getId(), user.getId()))
                    .willReturn(Optional.of(library));
            given(themeRepository.findById(theme.getId()))
                    .willReturn(Optional.of(theme));
            given(focusRepository.save(any(Focus.class)))
                    .willAnswer(invocation -> {
                        Focus saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 101L);
                        return saved;
                    });

            FocusResponseDto.FocusStart result = focusService.startFocus(user, request);

            verify(focusRepository).save(focusCaptor.capture());
            Focus saved = focusCaptor.getValue();
            assertThat(saved.getStartedAt()).isEqualTo(normalizedNow);
            assertThat(saved.getFocusDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(saved.getStartedTime()).isEqualTo(normalizedNow.toLocalTime());
            assertThat(result.startedAt()).isEqualTo(normalizedNow);
        }

        @Test
        @DisplayName("성공 - BEFORE에서 READING 전환 시 주입된 시작 날짜를 서재에 저장한다")
        void 성공_시작시서재시작날짜는주입된시각기준() {
            LocalDateTime serverNow = LocalDateTime.of(2026, 8, 1, 23, 55, 12, 987_654_321);
            useClock(serverNow);
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getId(), theme.getId());

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                    .willReturn(Optional.empty());
            given(libraryRepository.findByIdAndUserId(library.getId(), user.getId()))
                    .willReturn(Optional.of(library));
            given(themeRepository.findById(theme.getId()))
                    .willReturn(Optional.of(theme));
            given(focusRepository.save(any(Focus.class))).willAnswer(invocation -> invocation.getArgument(0));

            FocusResponseDto.FocusStart result = focusService.startFocus(user, request);

            assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 23, 55, 12));
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(library.getStartedAt()).isEqualTo(LocalDate.of(2026, 8, 1));
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
        @DisplayName("성공 - 자정을 넘은 세션을 일별 저장하고 원본 ID의 전체 응답을 반환한다")
        void 성공_자정분할저장과전체응답() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 55, 0, 900_000_000);
            LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 10, 0, 100_000_000);
            LocalDateTime normalizedStart = LocalDateTime.of(2026, 8, 1, 23, 55);
            LocalDateTime midnight = LocalDateTime.of(2026, 8, 2, 0, 0);
            LocalDateTime normalizedEnd = LocalDateTime.of(2026, 8, 2, 0, 10);
            Long originalFocusId = focus.getId();
            setFocusStartedAt(startedAt);
            useClock(endedAt);
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(originalFocusId, 72, true);
            List<Focus> persistedRows = stubSaveAllAndFlushWithGeneratedIds();

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(originalFocusId, user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(persistedRows).hasSize(2);
            Focus first = persistedRows.get(0);
            Focus last = persistedRows.get(1);
            assertThat(first).isSameAs(focus);
            assertThat(first.getId()).isEqualTo(originalFocusId);
            assertThat(first.getLibrary()).isSameAs(library);
            assertThat(first.getTheme()).isSameAs(theme);
            assertThat(first.getFocusDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(first.getStartedAt()).isEqualTo(normalizedStart);
            assertThat(first.getStartedTime()).isEqualTo(normalizedStart.toLocalTime());
            assertThat(first.getEndedAt()).isEqualTo(midnight);
            assertThat(first.getEndedTime()).isEqualTo(midnight.toLocalTime());
            assertThat(first.getDurationSec()).isEqualTo(300);
            assertThat(first.getEndPage()).isNull();
            assertThat(last.getId()).isEqualTo(101L);
            assertThat(last.getLibrary()).isSameAs(library);
            assertThat(last.getTheme()).isSameAs(theme);
            assertThat(last.getFocusDate()).isEqualTo(LocalDate.of(2026, 8, 2));
            assertThat(last.getStartedAt()).isEqualTo(midnight);
            assertThat(last.getStartedTime()).isEqualTo(midnight.toLocalTime());
            assertThat(last.getEndedAt()).isEqualTo(normalizedEnd);
            assertThat(last.getEndedTime()).isEqualTo(normalizedEnd.toLocalTime());
            assertThat(last.getDurationSec()).isEqualTo(600);
            assertThat(last.getEndPage()).isEqualTo(72);

            assertThat(library.getFocusSec()).isEqualTo(900L);
            assertThat(library.getPage()).isEqualTo(72);
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.FINISHED);
            assertThat(result.focusId()).isEqualTo(originalFocusId);
            assertThat(result.libraryId()).isEqualTo(library.getId());
            assertThat(result.startedAt()).isEqualTo(normalizedStart);
            assertThat(result.endedAt()).isEqualTo(normalizedEnd);
            assertThat(result.durationSec()).isEqualTo(900);
            assertThat(result.page()).isEqualTo(72);
            assertThat(result.totalFocusSec()).isEqualTo(900L);
            assertThat(result.readingStatus()).isEqualTo("FINISHED");

            ArgumentCaptor<Focus> timelineCaptor = ArgumentCaptor.forClass(Focus.class);
            verify(timelineCommandService, times(2)).appendFocusCompleted(timelineCaptor.capture());
            assertThat(timelineCaptor.getAllValues()).containsExactly(first, last);
            InOrder persistenceBeforeTimeline = inOrder(focusRepository, timelineCommandService);
            persistenceBeforeTimeline.verify(focusRepository).saveAllAndFlush(any());
            persistenceBeforeTimeline.verify(timelineCommandService).appendFocusCompleted(first);
            persistenceBeforeTimeline.verify(timelineCommandService).appendFocusCompleted(last);
            verify(eventPublisher, times(1)).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().equals(Set.of(YearMonth.of(2026, 8)))
            ));
        }

        @Test
        @DisplayName("성공 - 정확히 자정에 끝나면 이전 날짜 행만 저장한다")
        void 성공_정확한자정종료() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 8, 1, 23, 55);
            LocalDateTime endedAt = LocalDateTime.of(2026, 8, 2, 0, 0);
            setFocusStartedAt(startedAt);
            useClock(endedAt);
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);
            List<Focus> persistedRows = stubSaveAllAndFlushWithGeneratedIds();

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(persistedRows).containsExactly(focus);
            assertThat(focus.getFocusDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(focus.getStartedAt()).isEqualTo(startedAt);
            assertThat(focus.getEndedAt()).isEqualTo(endedAt);
            assertThat(focus.getDurationSec()).isEqualTo(300);
            assertThat(focus.getEndPage()).isEqualTo(45);
            assertThat(result.durationSec()).isEqualTo(300);
            verify(timelineCommandService, times(1)).appendFocusCompleted(focus);
        }

        @Test
        @DisplayName("성공 - 자정 양쪽의 마이크로초를 전체 초로 정규화한다")
        void 성공_자정마이크로초정규화() {
            LocalDateTime measuredStart = LocalDateTime.of(2026, 8, 1, 23, 59, 59, 500_000_000);
            LocalDateTime measuredEnd = LocalDateTime.of(2026, 8, 2, 0, 0, 0, 500_000_000);
            LocalDateTime normalizedStart = LocalDateTime.of(2026, 8, 1, 23, 59, 59);
            LocalDateTime normalizedEnd = LocalDateTime.of(2026, 8, 2, 0, 0);
            setFocusStartedAt(measuredStart);
            useClock(measuredEnd);
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);
            List<Focus> persistedRows = stubSaveAllAndFlushWithGeneratedIds();

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(persistedRows).containsExactly(focus);
            assertThat(focus.getFocusDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(focus.getStartedAt()).isEqualTo(normalizedStart);
            assertThat(focus.getEndedAt()).isEqualTo(normalizedEnd);
            assertThat(focus.getDurationSec()).isEqualTo(1);
            assertThat(result.startedAt()).isEqualTo(normalizedStart);
            assertThat(result.endedAt()).isEqualTo(normalizedEnd);
            assertThat(result.durationSec()).isEqualTo(1);
            assertThat(result.totalFocusSec()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공 - 자정 이후 완독 시 집계 종료 날짜를 서재에 저장한다")
        void 성공_자정분할완독시서재종료날짜는집계종료시각기준() {
            LocalDateTime measuredStart = LocalDateTime.of(2026, 8, 1, 23, 55, 12, 987_654_321);
            LocalDateTime measuredEnd = LocalDateTime.of(2026, 8, 2, 0, 10, 34, 123_456_789);
            setFocusStartedAt(measuredStart);
            useClock(measuredEnd);
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, true);
            stubSaveAllAndFlushWithGeneratedIds();

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(result.endedAt()).isEqualTo(LocalDateTime.of(2026, 8, 2, 0, 10, 34));
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.FINISHED);
            assertThat(library.getEndedAt()).isEqualTo(LocalDate.of(2026, 8, 2));
        }

        @Test
        @DisplayName("실패 - 같은 초로 정규화되면 어떤 상태나 부수 효과도 변경하지 않는다")
        void 실패_같은정규화초는무변경() {
            LocalDateTime measuredStart = LocalDateTime.of(2026, 8, 1, 12, 0, 0, 100_000_000);
            LocalDateTime measuredEnd = LocalDateTime.of(2026, 8, 1, 12, 0, 0, 900_000_000);
            setFocusStartedAt(measuredStart);
            useClock(measuredEnd);
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 99, true);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_DURATION_TOO_SHORT);

            assertThat(focus.getStartedAt()).isEqualTo(measuredStart);
            assertThat(focus.getEndedAt()).isNull();
            assertThat(focus.getDurationSec()).isZero();
            assertThat(focus.getEndPage()).isNull();
            assertThat(library.getFocusSec()).isZero();
            assertThat(library.getPage()).isZero();
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.BEFORE);
            verify(focusRepository, never()).saveAllAndFlush(any());
            verifyNoInteractions(timelineCommandService, eventPublisher);
        }

        @Test
        @DisplayName("실패 - 순차 재종료는 일별 행과 부수 효과를 중복 생성하지 않는다")
        void 실패_순차재종료중복없음() {
            setFocusStartedAt(LocalDateTime.of(2026, 8, 1, 23, 55));
            useClock(LocalDateTime.of(2026, 8, 2, 0, 10));
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, true);
            stubSaveAllAndFlushWithGeneratedIds();

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd firstResult = focusService.endFocus(user.getId(), request);

            assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_ENDED);
            assertThat(firstResult.durationSec()).isEqualTo(900);
            assertThat(library.getFocusSec()).isEqualTo(900L);
            assertThat(library.getPage()).isEqualTo(72);
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.FINISHED);
            verify(focusRepository, times(2))
                    .findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId());
            verify(focusRepository, times(1)).saveAllAndFlush(any());
            verify(timelineCommandService, times(2)).appendFocusCompleted(any(Focus.class));
            verify(eventPublisher, times(1)).publishEvent(any(LibraryCacheInvalidateEvent.class));
        }

        @Test
        @DisplayName("성공 - 완독 처리")
        void 성공_완독() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, true);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
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
                            && cacheEvent.affectedYearMonths().equals(Set.of(YearMonth.of(2026, 3)))
            ));
        }

        @Test
        @DisplayName("성공 - 완독하지 않으면 READING 유지")
        void 성공_독서중유지() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(result).isNotNull();
            assertThat(result.page()).isEqualTo(45);
            assertThat(focus.getEndPage()).isEqualTo(45);
            assertThat(result.readingStatus()).isEqualTo("READING");
            verify(timelineCommandService).appendFocusCompleted(focus);
            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && !cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().equals(Set.of(YearMonth.of(2026, 3)))
            ));
        }

        @Test
        @DisplayName("성공 - 종료의 기존 부수 효과를 유지한다")
        void 성공_종료의기존부수효과유지() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

            assertThat(focus.getEndedAt()).isEqualTo(result.endedAt());
            assertThat(library.getFocusSec()).isEqualTo(result.durationSec().longValue());
            assertThat(library.getPage()).isEqualTo(45);
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            verify(timelineCommandService, times(1)).appendFocusCompleted(focus);
        }

        @Test
        @DisplayName("성공 - 월초 자정 종료는 새 달 캐시를 무효화하지 않는다")
        void 성공_월초자정종료는새달제외() {
            setFocusStartedAt(LocalDateTime.of(2026, 3, 31, 23, 30));
            useClock(LocalDateTime.of(2026, 4, 1, 0, 0));
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            focusService.endFocus(user.getId(), request);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && !cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().equals(Set.of(YearMonth.of(2026, 3)))
            ));
        }

        @Test
        @DisplayName("성공 - 연도 경계 세션은 양쪽 달 캐시를 무효화한다")
        void 성공_연도경계세션은양쪽달무효화() {
            setFocusStartedAt(LocalDateTime.of(2025, 12, 31, 23, 30));
            useClock(LocalDateTime.of(2026, 1, 1, 0, 30));
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            focusService.endFocus(user.getId(), request);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && !cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                                    YearMonth.of(2025, 12),
                                    YearMonth.of(2026, 1)
                            ))
            ));
        }

        @Test
        @DisplayName("성공 - 여러 달 세션은 양의 겹침을 가진 모든 달 캐시를 무효화한다")
        void 성공_여러달세션은모든겹침달무효화() {
            setFocusStartedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
            useClock(LocalDateTime.of(2026, 4, 2, 10, 0));
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 45, false);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            focusService.endFocus(user.getId(), request);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(user.getId())
                            && !cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                                    YearMonth.of(2026, 1),
                                    YearMonth.of(2026, 2),
                                    YearMonth.of(2026, 3),
                                    YearMonth.of(2026, 4)
                            ))
            ));
        }

        @Test
        @DisplayName("실패 - 포커스를 찾을 수 없음")
        void 실패_포커스없음() {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(focus.getId(), 72, false);

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
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

            given(focusRepository.findByIdAndLibraryUserIdForUpdate(focus.getId(), user.getId()))
                    .willReturn(Optional.of(focus));

            assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_ENDED);
        }

        private List<Focus> stubSaveAllAndFlushWithGeneratedIds() {
            List<Focus> persistedRows = new ArrayList<>();
            given(focusRepository.saveAllAndFlush(any()))
                    .willAnswer(invocation -> {
                        List<Focus> rows = invocation.getArgument(0);
                        for (int index = 0; index < rows.size(); index++) {
                            Focus row = rows.get(index);
                            if (row.getId() == null) {
                                ReflectionTestUtils.setField(row, "id", 100L + index);
                            }
                        }
                        persistedRows.addAll(rows);
                        return rows;
                    });
            return persistedRows;
        }
    }
}
