package app.nook.focus.service;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.FocusFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.event.FocusTimelineAppendEvent;
import app.nook.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("FocusService 테스트")
class FocusServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private FocusRepository focusRepository;
    @Mock
    private LibraryRepository libraryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user;
    private Library library;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        Book book = FocusFixture.book();
        library = LibraryFixture.library(user, book);
    }

    @Nested
    @DisplayName("포커스 시작")
    class StartFocus {

        @Test
        @DisplayName("고정 Clock의 현재 시각을 초 단위로 절삭하고 BEFORE 상태 날짜를 맞춘다")
        void truncatesClockAndUsesNormalizedStartDate() {
            LocalDateTime now = LocalDateTime.of(2026, 8, 1, 23, 59, 59, 987_000_000);
            FocusService service = serviceAt(now);
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(library.getBook().getId());
            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId())).willReturn(Optional.empty());
            given(libraryRepository.findByUserIdAndBookId(user.getId(), library.getBook().getId()))
                    .willReturn(Optional.of(library));
            given(focusRepository.save(any(Focus.class))).willAnswer(invocation -> {
                Focus saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 101L);
                return saved;
            });

            FocusResponseDto.FocusStart result = service.startFocus(user, request);

            assertThat(result.focusId()).isEqualTo(101L);
            assertThat(result.bookId()).isEqualTo(library.getBook().getId());
            assertThat(result.bookTitle()).isEqualTo(library.getBook().getTitle());
            assertThat(result.author()).isEqualTo(library.getBook().getAuthor());
            assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 23, 59, 59));
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(library.getStartedAt()).isEqualTo(LocalDate.of(2026, 8, 1));
        }

        @Test
        @DisplayName("이미 진행 중인 포커스가 있으면 거절한다")
        void rejectsExistingActiveFocus() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 1, 10, 0));
            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId())).willReturn(Optional.of(active));

            assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 8, 1, 11, 0))
                    .startFocus(user, new FocusRequestDto.FocusStart(library.getBook().getId())))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
        }

        @Test
        @DisplayName("소유한 서재가 없으면 거절한다")
        void rejectsMissingLibrary() {
            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId())).willReturn(Optional.empty());
            given(libraryRepository.findByUserIdAndBookId(user.getId(), library.getBook().getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 8, 1, 11, 0))
                    .startFocus(user, new FocusRequestDto.FocusStart(library.getBook().getId())))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.LIBRARY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("포커스 종료")
    class EndFocus {

        @Test
        @DisplayName("페이지를 생략하면 기존 페이지를 보존하고 최종 분할 행의 종료 페이지도 비워 둔다")
        void preservesExistingPageWhenPageIsOmitted() {
            ReflectionTestUtils.setField(library, "page", 72);
            Focus active = activeFocus(library, LocalDateTime.of(2026, 8, 1, 23, 0));
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 8, 2, 0, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), null, false));

            List<Focus> rows = savedRows();
            assertThat(rows).hasSize(2);
            assertThat(rows.get(1).getEndPage()).isNull();
            assertThat(library.getPage()).isEqualTo(72);
            assertThat(result.page()).isEqualTo(72);
        }

        @Test
        @DisplayName("페이지 0 센티널은 응답에서 null로 변환한다")
        void mapsZeroPageSentinelToNull() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 1, 10, 0));
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), null, false));

            assertThat(result.page()).isNull();
        }

        @Test
        @DisplayName("같은 날 종료를 한 행과 한 월 이벤트로 저장한다")
        void completesSameDay() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 1, 10, 0));
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 30, false));

            assertThat(result.durationSec()).isEqualTo(1800);
            assertThat(result.bookId()).isEqualTo(library.getBook().getId());
            assertThat(active.getDurationSec()).isEqualTo(1800);
            assertThat(active.getEndPage()).isEqualTo(30);
            verifyMonthlyEvent(Set.of(java.time.YearMonth.of(2026, 8)), false);
        }

        @Test
        @DisplayName("23시부터 다음 날 00시 30분까지 원본과 새 행으로 나누고 aggregate를 반환한다")
        void splitsAcrossMidnightAndAppliesLibraryOnce() {
            Library trackedLibrary = spy(library);
            ReflectionTestUtils.setField(trackedLibrary, "readingStatus", ReadingStatus.BEFORE);
            Focus active = activeFocus(trackedLibrary, LocalDateTime.of(2026, 8, 1, 23, 0));
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 8, 2, 0, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 72, false));

            List<Focus> rows = savedRows();
            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)).isSameAs(active);
            assertThat(rows.get(0).getDurationSec()).isEqualTo(3600);
            assertThat(rows.get(0).getEndPage()).isNull();
            assertThat(rows.get(1).getDurationSec()).isEqualTo(1800);
            assertThat(rows.get(1).getEndPage()).isEqualTo(72);
            assertThat(rows.get(1).getLibrary()).isSameAs(trackedLibrary);
            assertThat(result.focusId()).isEqualTo(active.getId());
            assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 23, 0));
            assertThat(result.endedAt()).isEqualTo(LocalDateTime.of(2026, 8, 2, 0, 30));
            assertThat(result.durationSec()).isEqualTo(5400);
            assertThat(result.page()).isEqualTo(72);
            assertThat(result.totalFocusSec()).isEqualTo(5400L);
            assertThat(result.readingStatus()).isEqualTo("READING");
            assertThat(trackedLibrary.getStartedAt()).isEqualTo(LocalDate.of(2026, 8, 2));
            verify(trackedLibrary).recordFocus(5400);
            verify(trackedLibrary).recordPage(72);
            verify(trackedLibrary).updateStatus(ReadingStatus.READING, LocalDate.of(2026, 8, 2));
            verifyTimelineEvent(List.of(rows.get(0).getId(), rows.get(1).getId()));
        }

        @Test
        @DisplayName("월초 자정에 정확히 끝나면 새 달을 영향 월에서 제외한다")
        void excludesMonthAtExactEndBoundary() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 31, 23, 0));
            stubOwnedAndGeneratedIds(active);

            serviceAt(LocalDateTime.of(2026, 9, 1, 0, 0))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 20, false));

            assertThat(savedRows()).hasSize(1);
            verifyMonthlyEvent(Set.of(java.time.YearMonth.of(2026, 8)), false);
        }

        @Test
        @DisplayName("월 경계를 넘으면 시작 월과 종료 월을 모두 무효화한다")
        void includesBothCrossedMonths() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 31, 23, 0));
            stubOwnedAndGeneratedIds(active);

            serviceAt(LocalDateTime.of(2026, 9, 1, 0, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 20, false));

            verifyMonthlyEvent(Set.of(java.time.YearMonth.of(2026, 8), java.time.YearMonth.of(2026, 9)), false);
        }

        @Test
        @DisplayName("완독은 종료 정규화 날짜와 월별·온보딩 결합 이벤트를 한 번 사용한다")
        void finishesWithCombinedEventOnce() {
            Focus active = activeFocus(LocalDateTime.of(2026, 8, 31, 23, 0));
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 9, 1, 0, 30))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 120, true));

            assertThat(result.readingStatus()).isEqualTo("FINISHED");
            assertThat(library.getEndedAt()).isEqualTo(LocalDate.of(2026, 9, 1));
            verifyMonthlyEvent(Set.of(java.time.YearMonth.of(2026, 8), java.time.YearMonth.of(2026, 9)), true);
        }

        @Test
        @DisplayName("없는 포커스는 잠금 소유권 조회에서 숨긴다")
        void rejectsMissingFocus() {
            given(focusRepository.findByIdAndLibraryUserIdForUpdate(100L, user.getId())).willReturn(Optional.empty());

            assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 8, 1, 11, 0))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(100L, 10, false)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_NOT_FOUND);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("이미 종료된 포커스는 추가 저장 없이 거절한다")
        void rejectsAlreadyEndedFocus() {
            Focus completed = FocusFixture.completedFocus(library);
            given(focusRepository.findByIdAndLibraryUserIdForUpdate(completed.getId(), user.getId()))
                    .willReturn(Optional.of(completed));

            assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 8, 1, 11, 0))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(completed.getId(), 10, false)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FocusErrorCode.FOCUS_ALREADY_ENDED);
            verify(focusRepository, never()).saveAllAndFlush(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("정규화 후 1초 미만이면 0초 Focus로 저장한다")
        void savesSubsecondAsZeroDuration() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 9, 1, 0, 0, 0, 700_000_000);
            Focus active = activeFocus(startedAt);
            stubOwnedAndGeneratedIds(active);

            FocusResponseDto.FocusEnd result = serviceAt(LocalDateTime.of(2026, 9, 1, 0, 0, 0, 900_000_000))
                    .endFocus(user.getId(), new FocusRequestDto.FocusEnd(active.getId(), 99, false));

            assertThat(savedRows()).containsExactly(active);
            assertThat(active.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
            assertThat(active.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
            assertThat(active.getDurationSec()).isZero();
            assertThat(active.getEndPage()).isEqualTo(99);
            assertThat(result.durationSec()).isZero();
            assertThat(result.page()).isEqualTo(99);
            verifyMonthlyEvent(Set.of(java.time.YearMonth.of(2026, 9)), false);
            verifyTimelineEvent(List.of(active.getId()));
        }
    }

    private FocusService serviceAt(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(KST).toInstant();
        return new FocusService(
                focusRepository,
                libraryRepository,
                eventPublisher,
                Clock.fixed(instant, KST),
                new FocusCompletionSegmenter()
        );
    }

    private Focus activeFocus(LocalDateTime startedAt) {
        return activeFocus(library, startedAt);
    }

    private Focus activeFocus(Library focusLibrary, LocalDateTime startedAt) {
        Focus active = Focus.builder()
                .library(focusLibrary)
                .startedAt(startedAt)
                .durationSec(0)
                .build();
        ReflectionTestUtils.setField(active, "id", 100L);
        return active;
    }

    private void stubOwnedAndGeneratedIds(Focus active) {
        given(focusRepository.findByIdAndLibraryUserIdForUpdate(active.getId(), user.getId()))
                .willReturn(Optional.of(active));
        given(focusRepository.saveAllAndFlush(any())).willAnswer(invocation -> {
            List<Focus> rows = invocation.getArgument(0);
            long nextId = 101L;
            for (Focus row : rows) {
                if (row.getId() == null) {
                    ReflectionTestUtils.setField(row, "id", nextId++);
                }
            }
            return rows;
        });
    }

    @SuppressWarnings("unchecked")
    private List<Focus> savedRows() {
        ArgumentCaptor<List<Focus>> captor = ArgumentCaptor.forClass(List.class);
        verify(focusRepository).saveAllAndFlush(captor.capture());
        return captor.getValue();
    }

    private void verifyMonthlyEvent(Set<java.time.YearMonth> expectedMonths, boolean onboarding) {
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof LibraryCacheInvalidateEvent cacheEvent
                        && cacheEvent.userId().equals(user.getId())
                        && cacheEvent.affectedYearMonths().equals(expectedMonths)
                        && cacheEvent.evictOnboardingGoal() == onboarding
        ));
    }

    private void verifyTimelineEvent(List<Long> expectedFocusIds) {
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof FocusTimelineAppendEvent timelineEvent
                        && timelineEvent.focusIds().equals(expectedFocusIds)
        ));
    }
}
