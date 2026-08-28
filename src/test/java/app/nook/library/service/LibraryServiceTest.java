package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.book.service.BookAccessService;
import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.service.FocusDailyTimeCalculator;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.BookFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.dto.ReadingStatusResponse;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.library.repository.dto.LibraryBookQueryResult;
import app.nook.library.repository.dto.LibraryStatusCount;
import app.nook.library.util.LibraryBookCursorCodec;
import app.nook.r2.service.PresignedUrlService;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Library 서비스 테스트")
class LibraryServiceTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private TimelineCommandService timelineCommandService;

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookAccessService bookAccessService;

    @Mock
    private Clock clock;

    @Spy
    private FocusDailyTimeCalculator focusDailyTimeCalculator = new FocusDailyTimeCalculator();

    @InjectMocks
    private LibraryCommandService libraryCommandService;

    @InjectMocks
    private LibraryQueryService libraryQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(presignedUrlService.resolveImageUrl(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDateTime serverNow = LocalDateTime.of(2026, 3, 2, 12, 0);
        lenient().when(clock.instant()).thenReturn(serverNow.atZone(kst).toInstant());
        lenient().when(clock.getZone()).thenReturn(kst);
    }

    @Nested
    @DisplayName("서재 책 등록")
    class Save {

        @Test
        @DisplayName("성공")
        void 책_등록_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());
            given(libraryRepository.saveAndFlush(any(Library.class))).willAnswer(invocation -> {
                Library saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                ReflectionTestUtils.setField(saved, "createdDate", LocalDateTime.now());
                return saved;
            });

            LibraryViewDto.BookStatusResponseDto response =
                    libraryCommandService.registerBook(1L, 1L);

            assertThat(response.bookId()).isEqualTo(1L);
            assertThat(response.bookShelfId()).isEqualTo(1L);
            assertThat(response.libraryId()).isEqualTo(1L);
            assertThat(response.readingStatus()).isEqualTo(ReadingStatusResponse.BEFORE);
            verify(timelineCommandService).appendRegister(any());
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void save_도서없음_예외() {
            User user = UserFixture.user();
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.registerBook(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 등록된 도서면 예외를 던진다")
        void save_이미등록_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.registerBook(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_ALREADY_EXIST);
        }

        @Test
        @DisplayName("접근 권한이 없는 도서면 예외를 던진다")
        void save_도서접근권한없음_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            willThrow(new CustomException(BookErrorCode.BOOK_ACCESS_DENIED))
                    .given(bookAccessService).assertCanAddToLibrary(user, book);

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.registerBook(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_ACCESS_DENIED);
            verify(libraryRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("서재 책 삭제")
    class DeleteById {

        @Test
        @DisplayName("성공")
        void deleteById_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            LibraryViewDto.BookStatusResponseDto response =
                    libraryCommandService.deleteByBookId(1L, 1L);

            assertThat(response.bookId()).isEqualTo(1L);
            assertThat(response.bookShelfId()).isNull();
            assertThat(response.libraryId()).isNull();
            assertThat(response.readingStatus()).isEqualTo(ReadingStatusResponse.UNREGISTERED);
            verify(libraryRepository).delete(library);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void deleteById_서재없음_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.deleteByBookId(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void deleteById_도서없음_예외() {
            User user = UserFixture.user();
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.deleteByBookId(1L, 1L));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제 시 사용자 기준 월별 캐시 무효화 이벤트를 발행한다")
        void deleteById_캐시무효화_이벤트발행() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            Focus februaryFocus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 2, 1, 10, 0))
                    .endedAt(LocalDateTime.of(2026, 2, 1, 10, 30))
                    .durationSec(1800)
                    .build();
            Focus marchFocus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                    .endedAt(LocalDateTime.of(2026, 3, 1, 10, 30))
                    .durationSec(1800)
                    .build();
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L))
                    .willReturn(List.of(februaryFocus, marchFocus));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(1L)
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                            java.time.YearMonth.of(2026, 2),
                            java.time.YearMonth.of(2026, 3)
                            ))
                            && !cacheEvent.evictOnboardingGoal()
            ));
        }

        @Test
        @DisplayName("삭제 시 영향 월 계산을 위해 포커스 날짜를 조회한다")
        void deleteById_포커스날짜조회후_삭제() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            Focus focus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 2, 1, 10, 0))
                    .endedAt(LocalDateTime.of(2026, 2, 1, 10, 30))
                    .durationSec(1800)
                    .build();
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L)).willReturn(List.of(focus));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(focusRepository).findAllByLibraryIdAndLibraryUserId(10L, 1L);
            verify(libraryRepository).delete(library);
        }

        @Test
        @DisplayName("삭제 시 자정을 넘긴 포커스의 모든 영향 월을 포함한다")
        void deleteByBookId_usesFocusIntervalsForAffectedMonths() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            Focus focus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 1, 31, 23, 30))
                    .endedAt(LocalDateTime.of(2026, 2, 1, 0, 30))
                    .durationSec(3600)
                    .build();
            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L)).willReturn(List.of(focus));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                            java.time.YearMonth.of(2026, 1),
                            java.time.YearMonth.of(2026, 2)
                    ))
            ));
        }

        @Test
        @DisplayName("정확히 월초 자정에 종료된 포커스는 다음 달에 포함하지 않는다")
        void deleteByBookId_exactMidnightExcludesNextMonth() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            Focus focus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 3, 31, 23, 30))
                    .endedAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .durationSec(1800)
                    .build();
            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L)).willReturn(List.of(focus));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.affectedYearMonths().equals(Set.of(java.time.YearMonth.of(2026, 3)))
            ));
        }

        @Test
        @DisplayName("여러 포커스가 영향을 주는 월을 중복 없이 합산한다")
        void deleteByBookId_multiMonthUnion() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            Focus first = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .endedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                    .durationSec(0)
                    .build();
            Focus second = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 3, 31, 23, 30))
                    .endedAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                    .durationSec(0)
                    .build();
            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L)).willReturn(List.of(first, second));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                            java.time.YearMonth.of(2026, 1),
                            java.time.YearMonth.of(2026, 2),
                            java.time.YearMonth.of(2026, 3),
                            java.time.YearMonth.of(2026, 4)
                    ))
            ));
        }

        @Test
        @DisplayName("일별로 저장된 포커스 행은 영향 월을 중복 없이 합산한다")
        void deleteByBookId_dailyFocusRowsUnionAffectedMonthsWithoutDuplicates() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            Focus januaryDailyRow = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 1, 31, 23, 30))
                    .endedAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                    .durationSec(1800)
                    .build();
            Focus februaryDailyRow = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                    .endedAt(LocalDateTime.of(2026, 2, 1, 0, 30))
                    .durationSec(1800)
                    .build();
            Focus anotherFebruaryDailyRow = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 2, 15, 10, 0))
                    .endedAt(LocalDateTime.of(2026, 2, 15, 10, 30))
                    .durationSec(1800)
                    .build();
            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L))
                    .willReturn(List.of(januaryDailyRow, februaryDailyRow, anotherFebruaryDailyRow));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher, times(1)).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(1L)
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                            java.time.YearMonth.of(2026, 1),
                            java.time.YearMonth.of(2026, 2)
                    ))
            ));
        }

        @Test
        @DisplayName("진행 중 포커스의 영향 월은 KST 서버 현재 시각으로 계산한다")
        void deleteByBookId_ongoingUsesKstServerNow() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            Focus focus = Focus.builder()
                    .library(library)
                    .startedAt(LocalDateTime.of(2026, 2, 28, 23, 30))
                    .endedAt(null)
                    .durationSec(0)
                    .build();
            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));
            given(focusRepository.findAllByLibraryIdAndLibraryUserId(10L, 1L)).willReturn(List.of(focus));

            libraryCommandService.deleteByBookId(1L, 1L);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.affectedYearMonths().equals(Set.of(
                            java.time.YearMonth.of(2026, 2),
                            java.time.YearMonth.of(2026, 3)
                    ))
            ));
            verify(focusDailyTimeCalculator).affectedYearMonths(
                    focus.getStartedAt(),
                    null,
                    LocalDateTime.of(2026, 3, 2, 12, 0)
            );
        }
    }

    @Nested
    @DisplayName("서재 독서 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("성공")
        void changeStatus_성공() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "id", 10L);

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            LibraryViewDto.BookStatusResponseDto response =
                    libraryCommandService.changeReadingStatus(1L, request);

            assertThat(response.bookId()).isEqualTo(1L);
            assertThat(response.bookShelfId()).isEqualTo(library.getId());
            assertThat(response.libraryId()).isEqualTo(library.getId());
            assertThat(response.readingStatus()).isEqualTo(ReadingStatusResponse.READING);
            assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
            verify(timelineCommandService).appendStatusChanged(any(), any());
        }

        @Test
        @DisplayName("중복 상태 변경 요청이면 예외를 던진다")
        void changeStatus_중복상태_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_STATUS_INVALID);
        }

        @Test
        @DisplayName("서재에 책이 없으면 예외를 던진다")
        void changeStatus_서재없음_예외() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("도서가 없으면 예외를 던진다")
        void changeStatus_도서없음_예외() {
            User user = UserFixture.user();
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> libraryCommandService.changeReadingStatus(1L, request));

            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("완독 수가 바뀌지 않는 상태 변경 성공 시 캐시 무효화 이벤트는 발행하지 않는다")
        void changeStatus_성공_이벤트미발행() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.changeReadingStatus(1L, request);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("완독 수가 바뀌는 상태 변경 성공 시 온보딩 목표 캐시 무효화 이벤트를 발행한다")
        void changeStatus_finishedCountChanged_온보딩목표캐시무효화_이벤트발행() {
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.FINISHED);

            given(bookRepository.findById(1L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserIdAndBook(1L, book)).willReturn(Optional.of(library));

            libraryCommandService.changeReadingStatus(1L, request);

            verify(eventPublisher).publishEvent(argThat((Object event) ->
                    event instanceof LibraryCacheInvalidateEvent cacheEvent
                            && cacheEvent.userId().equals(1L)
                            && cacheEvent.evictOnboardingGoal()
                            && cacheEvent.affectedYearMonths().isEmpty()
            ));
        }
    }

    @Nested
    @DisplayName("책 상태별 조회")
    class ViewBooksByStatus {

        @Test
        @DisplayName("첫 조회도 목록만 반환하고 전체 개수를 조회하지 않는다")
        void viewBooksByStatus_첫조회_목록만반환() {
            User user = UserFixture.user();

            Book book1 = BookFixture.book();
            ReflectionTestUtils.setField(book1, "id", 1L);
            ReflectionTestUtils.setField(book1, "title", "테스트 도서1");
            ReflectionTestUtils.setField(book1, "author", "작가1");
            ReflectionTestUtils.setField(book1, "coverImageKey", "book/users/1/cover1.png");

            Book book2 = BookFixture.book();
            ReflectionTestUtils.setField(book2, "id", 2L);
            ReflectionTestUtils.setField(book2, "isbn13", "1234567890124");
            ReflectionTestUtils.setField(book2, "title", "테스트 도서2");
            ReflectionTestUtils.setField(book2, "author", "작가2");
            ReflectionTestUtils.setField(book2, "coverImageKey", "https://example.com/cover2.jpg");

            Library library1 = LibraryFixture.library(user, book1);
            ReflectionTestUtils.setField(library1, "id", 10L);
            ReflectionTestUtils.setField(library1, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library1, "startedAt", LocalDate.of(2025, 1, 1));

            Library library2 = LibraryFixture.library(user, book2);
            ReflectionTestUtils.setField(library2, "id", 9L);
            ReflectionTestUtils.setField(library2, "readingStatus", ReadingStatus.READING);
            ReflectionTestUtils.setField(library2, "startedAt", LocalDate.of(2025, 1, 2));

            int size = 1;
            Slice<Library> slice = new SliceImpl<>(List.of(library1, library2), PageRequest.of(0, size + 1), true);

            given(libraryRepository.findByUserIdAndStatusWithCursor(anyLong(), any(), any(), any())).willReturn(slice);
            willReturn("https://r2.example.com/cover1.png")
                    .given(presignedUrlService)
                    .resolveImageUrl(1L, "book/users/1/cover1.png");

            LibraryViewDto.StatusBookResponseDto response =
                    libraryQueryService.getBooksByStatus(1L, ReadingStatus.READING, null, size);

            assertThat(response.readingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(response.bookItems().items()).hasSize(1);
            assertThat(response.bookItems().items().get(0).coverUrl())
                    .isEqualTo("https://r2.example.com/cover1.png");
            verify(libraryRepository, never()).countByUserIdGroupByReadingStatus(anyLong());
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/cover1.png");
        }

        @Test
        @DisplayName("상태별 책 개수는 한 번의 집계 결과로 반환하고 없는 상태는 0으로 반환한다")
        void getBookCountsByStatus_상태별개수반환() {
            given(libraryRepository.countByUserIdGroupByReadingStatus(1L)).willReturn(List.of(
                    new LibraryStatusCount(ReadingStatus.BEFORE, 3L),
                    new LibraryStatusCount(ReadingStatus.READING, 1L)
            ));

            LibraryViewDto.StatusBookCountsResponseDto response =
                    libraryQueryService.getBookCountsByStatus(1L);

            assertThat(response.before()).isEqualTo(3L);
            assertThat(response.reading()).isEqualTo(1L);
            assertThat(response.finished()).isZero();
            verify(libraryRepository).countByUserIdGroupByReadingStatus(1L);
        }
    }

    @Nested
    @DisplayName("책 개수 조회")
    class CountBooks {
        @Test
        @DisplayName("정상")
        void countBooks_정상() {
            User user = UserFixture.user();
            given(libraryRepository.countByUserId(1L)).willReturn(7);

            LibraryViewDto.BookCountResponseDto result = libraryQueryService.getBookCount(1L);

            assertThat(result.totalBookNum()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("서재 전체 책 목록 조회")
    class ViewAllBooks {

        @Test
        @DisplayName("다음 페이지가 있으면 size만큼 반환하고 정렬 기준에 맞는 다음 커서를 만든다")
        void viewAllBooks_다음페이지_존재() {
            LocalDateTime focusedAt = LocalDateTime.of(2026, 3, 1, 10, 0);
            LibraryBookQueryResult first = new LibraryBookQueryResult(
                    10L,
                    1L,
                    "첫 번째",
                    "작가1",
                    "book/users/1/first.png",
                    ReadingStatus.READING,
                    focusedAt,
                    0L
            );
            LibraryBookQueryResult second = new LibraryBookQueryResult(
                    9L,
                    2L,
                    "두 번째",
                    "작가2",
                    "book/users/1/second.png",
                    ReadingStatus.BEFORE,
                    LocalDateTime.of(2026, 2, 1, 10, 0),
                    0L
            );
            given(libraryRepository.findAllBooksByCursor(1L, null, LibrarySortType.RECENT_FOCUSED, 1))
                    .willReturn(List.of(first, second));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/first.png"))
                    .willReturn("https://cdn.example.com/first.png");

            CursorResponse<LibraryViewDto.LibraryBookItem, String> result =
                    libraryQueryService.getAllBooks(1L, null, LibrarySortType.RECENT_FOCUSED, 1);

            LibraryBookCursor decodedCursor = LibraryBookCursorCodec.decode(result.nextCursor());
            assertThat(result.hasNext()).isTrue();
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).bookId()).isEqualTo(1L);
            assertThat(result.items().get(0).coverUrl()).isEqualTo("https://cdn.example.com/first.png");
            assertThat(decodedCursor.libraryId()).isEqualTo(10L);
            assertThat(decodedCursor.lastFocusedAt()).isEqualTo(focusedAt);
            assertThat(decodedCursor.recordCount()).isNull();
            assertThat(decodedCursor.title()).isNull();
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/first.png");
        }

        @Test
        @DisplayName("기록 개수순 조회는 recordCount 기반 다음 커서를 만든다")
        void viewAllBooks_기록개수순_다음커서() {
            LibraryBookQueryResult first = new LibraryBookQueryResult(
                    20L,
                    1L,
                    "기록 많은 책",
                    "작가",
                    "cover",
                    ReadingStatus.FINISHED,
                    null,
                    3L
            );
            LibraryBookQueryResult second = new LibraryBookQueryResult(
                    19L,
                    2L,
                    "다음 책",
                    "작가",
                    "cover2",
                    ReadingStatus.READING,
                    null,
                    2L
            );
            given(libraryRepository.findAllBooksByCursor(
                    eq(1L),
                    any(),
                    eq(LibrarySortType.RECORD_COUNT_DESC),
                    eq(1)
            )).willReturn(List.of(first, second));

            CursorResponse<LibraryViewDto.LibraryBookItem, String> result =
                    libraryQueryService.getAllBooks(
                            1L,
                            new LibraryBookCursor(30L, null, 5L, null),
                            LibrarySortType.RECORD_COUNT_DESC,
                            1
                    );

            LibraryBookCursor decodedCursor = LibraryBookCursorCodec.decode(result.nextCursor());
            assertThat(decodedCursor.libraryId()).isEqualTo(20L);
            assertThat(decodedCursor.recordCount()).isEqualTo(3L);
            assertThat(decodedCursor.lastFocusedAt()).isNull();
            assertThat(decodedCursor.title()).isNull();
        }

        @Test
        @DisplayName("가나다순 조회는 title 기반 다음 커서를 만든다")
        void viewAllBooks_가나다순_다음커서() {
            LibraryBookQueryResult first = new LibraryBookQueryResult(
                    1L,
                    1L,
                    "가나다",
                    "작가",
                    "cover",
                    ReadingStatus.BEFORE,
                    null,
                    0L
            );
            LibraryBookQueryResult second = new LibraryBookQueryResult(
                    2L,
                    2L,
                    "라마바",
                    "작가",
                    "cover2",
                    ReadingStatus.READING,
                    null,
                    0L
            );
            given(libraryRepository.findAllBooksByCursor(1L, null, LibrarySortType.ALPHABETICAL, 1))
                    .willReturn(List.of(first, second));

            CursorResponse<LibraryViewDto.LibraryBookItem, String> result =
                    libraryQueryService.getAllBooks(1L, null, LibrarySortType.ALPHABETICAL, 1);

            LibraryBookCursor decodedCursor = LibraryBookCursorCodec.decode(result.nextCursor());
            assertThat(decodedCursor.libraryId()).isEqualTo(1L);
            assertThat(decodedCursor.title()).isEqualTo("가나다");
            assertThat(decodedCursor.lastFocusedAt()).isNull();
            assertThat(decodedCursor.recordCount()).isNull();
        }

        @Test
        @DisplayName("마지막 페이지면 다음 커서 없이 전체 결과를 반환한다")
        void viewAllBooks_마지막페이지() {
            LibraryBookQueryResult only = new LibraryBookQueryResult(
                    1L,
                    1L,
                    "마지막 책",
                    "작가",
                    "cover",
                    ReadingStatus.BEFORE,
                    null,
                    0L
            );
            given(libraryRepository.findAllBooksByCursor(1L, null, LibrarySortType.ALPHABETICAL, 20))
                    .willReturn(List.of(only));

            CursorResponse<LibraryViewDto.LibraryBookItem, String> result =
                    libraryQueryService.getAllBooks(1L, null, LibrarySortType.ALPHABETICAL, 20);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findOwnedIsbns")
    class FindOwnedIsbns {
        @Test
        @DisplayName("정상")
        void findOwnedIsbns_정상() {
            Long userId = 1L;
            List<String> isbns = List.of("978123", "978456");
            Set<String> owned = Set.of("978123");

            given(libraryRepository.findAladinIsbnsByUserIdAndIsbnIn(userId, isbns)).willReturn(owned);

            Set<String> result = libraryQueryService.getOwnedIsbns(userId, isbns);

            assertThat(result).containsExactly("978123");
        }

        @Test
        @DisplayName("ISBN 목록이 비어 있으면 repository를 호출하지 않는다")
        void findOwnedIsbns_빈목록() {
            Set<String> result = libraryQueryService.getOwnedIsbns(1L, List.of());

            assertThat(result).isEmpty();
            verify(libraryRepository, never()).findAladinIsbnsByUserIdAndIsbnIn(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("서재 책 검색")
    class SearchBooksInLibrary {
        @Test
        @DisplayName("키워드 이스케이프를 적용해 조회한다")
        void searchBooksInLibrary_키워드_이스케이프_정상() {
            Long userId = 1L;
            String rawKeyword = "A\\B%_C";
            String escapedKeyword = "A\\\\B\\%\\_C";
            int page = 0;
            int size = 10;

            Page<Library> expected = new PageImpl<>(List.of());
            given(libraryRepository.searchByUserIdAndKeyword(eq(userId), eq(escapedKeyword), any(PageRequest.class)))
                    .willReturn(expected);

            Page<Library> result = libraryQueryService.searchBooksInLibrary(userId, rawKeyword, page, size);

            assertThat(result).isEqualTo(expected);
            verify(libraryRepository).searchByUserIdAndKeyword(eq(userId), eq(escapedKeyword), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("포커스 기록 조회")
    class ViewFocusRecordByDate {

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext=true와 nextCursor를 반환한다")
        void viewFocusRecordByDate_다음페이지_존재() {
            User user = UserFixture.user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book1 = BookFixture.book();
            ReflectionTestUtils.setField(book1, "id", 1L);
            ReflectionTestUtils.setField(book1, "title", "도서1");
            ReflectionTestUtils.setField(book1, "author", "작가1");
            ReflectionTestUtils.setField(book1, "coverImageKey", "cover1");
            Book book2 = BookFixture.book();
            ReflectionTestUtils.setField(book2, "id", 2L);
            ReflectionTestUtils.setField(book2, "isbn13", "1234567890124");
            ReflectionTestUtils.setField(book2, "title", "도서2");
            ReflectionTestUtils.setField(book2, "author", "작가2");
            ReflectionTestUtils.setField(book2, "coverImageKey", "cover2");

            Library library1 = LibraryFixture.library(user, book1);
            Library library2 = LibraryFixture.library(user, book2);

            Focus focus1 = new Focus();
            ReflectionTestUtils.setField(focus1, "id", 30L);
            ReflectionTestUtils.setField(focus1, "library", library1);
            ReflectionTestUtils.setField(focus1, "startedAt", LocalDateTime.of(2026, 3, 1, 10, 0));
            ReflectionTestUtils.setField(focus1, "endedAt", LocalDateTime.of(2026, 3, 1, 10, 2));
            ReflectionTestUtils.setField(focus1, "durationSec", 120);

            Focus focus2 = new Focus();
            ReflectionTestUtils.setField(focus2, "id", 29L);
            ReflectionTestUtils.setField(focus2, "library", library2);
            ReflectionTestUtils.setField(focus2, "startedAt", LocalDateTime.of(2026, 3, 1, 11, 0));
            ReflectionTestUtils.setField(focus2, "endedAt", LocalDateTime.of(2026, 3, 1, 11, 30));
            ReflectionTestUtils.setField(focus2, "durationSec", null);

            int size = 1;
            Slice<Focus> slice = new SliceImpl<>(List.of(focus1, focus2), PageRequest.of(0, size + 1), true);
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user),
                    eq(date),
                    eq(LocalDateTime.of(2026, 3, 2, 12, 0)),
                    isNull(),
                    any(PageRequest.class)
            ))
                    .willReturn(slice);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            var result = libraryQueryService.getFocusRecordsByDate(1L, date, null, size);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(30L);
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).focusTime()).isEqualTo("00:02:00");
        }

        @Test
        @DisplayName("마지막 페이지면 hasNext=false이며 시작·종료 시각이 같은 포커스는 00:00:00을 반환한다")
        void viewFocusRecordByDate_마지막페이지_및_길이가0인포커스_처리() {
            User user = UserFixture.user();
            LocalDate date = LocalDate.of(2026, 3, 1);

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            ReflectionTestUtils.setField(book, "title", "도서1");
            ReflectionTestUtils.setField(book, "author", "작가1");
            ReflectionTestUtils.setField(book, "coverImageKey", "cover1");

            Library library = LibraryFixture.library(user, book);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 10L);
            ReflectionTestUtils.setField(focus, "library", library);
            ReflectionTestUtils.setField(focus, "startedAt", LocalDateTime.of(2026, 3, 1, 12, 0));
            ReflectionTestUtils.setField(focus, "endedAt", LocalDateTime.of(2026, 3, 1, 12, 0));

            int size = 2;
            Slice<Focus> slice = new SliceImpl<>(List.of(focus), PageRequest.of(0, size + 1), false);
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user),
                    eq(date),
                    eq(LocalDateTime.of(2026, 3, 2, 12, 0)),
                    eq(100L),
                    any(PageRequest.class)
            ))
                    .willReturn(slice);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            var result = libraryQueryService.getFocusRecordsByDate(1L, date, 100L, size);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).focusTime()).isEqualTo("00:00:00");
        }

        @Test
        @DisplayName("자정을 넘긴 포커스는 요청 날짜에 해당하는 시간만 반환한다")
        void viewFocusRecordByDate_자정분할() {
            User user = UserFixture.user();
            LocalDate date = LocalDate.of(2026, 3, 1);
            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 1L);
            ReflectionTestUtils.setField(book, "title", "도서1");
            ReflectionTestUtils.setField(book, "author", "작가1");
            ReflectionTestUtils.setField(book, "coverImageKey", "cover1");
            Library library = LibraryFixture.library(user, book);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 10L);
            ReflectionTestUtils.setField(focus, "library", library);
            ReflectionTestUtils.setField(focus, "startedAt", LocalDateTime.of(2026, 2, 28, 23, 55));
            ReflectionTestUtils.setField(focus, "endedAt", LocalDateTime.of(2026, 3, 1, 0, 10));
            ReflectionTestUtils.setField(focus, "durationSec", 900);

            Slice<Focus> slice = new SliceImpl<>(List.of(focus), PageRequest.of(0, 3), false);
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user),
                    eq(date),
                    eq(LocalDateTime.of(2026, 3, 2, 12, 0)),
                    isNull(),
                    any(PageRequest.class)
            ))
                    .willReturn(slice);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            var result = libraryQueryService.getFocusRecordsByDate(1L, date, null, 2);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).focusTime()).isEqualTo("00:10:00");
        }

        @Test
        @DisplayName("미래 날짜의 포커스 기록 조회는 빈 결과를 반환한다")
        void viewFocusRecordByDate_futureDateReturnsEmpty() {
            User user = UserFixture.user();
            LocalDate futureDate = LocalDate.of(2026, 3, 3);
            LocalDateTime serverNow = LocalDateTime.of(2026, 3, 2, 12, 0);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user), eq(futureDate), eq(serverNow), isNull(), any(PageRequest.class)
            )).willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 2), false));

            CursorResponse<LibraryViewDto.UserBookResponseDto, Long> result =
                    libraryQueryService.getFocusRecordsByDate(1L, futureDate, null, 1);

            assertThat(result.items()).isEmpty();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.hasNext()).isFalse();
            verify(presignedUrlService, never()).resolveImageUrl(anyLong(), any());
        }

        @Test
        @DisplayName("커서가 있어도 미래 날짜의 포커스 기록 조회는 빈 결과를 반환한다")
        void viewFocusRecordByDate_futureDateWithCursorReturnsEmpty() {
            User user = UserFixture.user();
            LocalDate futureDate = LocalDate.of(2026, 3, 3);
            LocalDateTime serverNow = LocalDateTime.of(2026, 3, 2, 12, 0);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user), eq(futureDate), eq(serverNow), eq(99L), any(PageRequest.class)
            )).willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 2), false));

            CursorResponse<LibraryViewDto.UserBookResponseDto, Long> result =
                    libraryQueryService.getFocusRecordsByDate(1L, futureDate, 99L, 1);

            assertThat(result.items()).isEmpty();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.hasNext()).isFalse();
            verify(presignedUrlService, never()).resolveImageUrl(anyLong(), any());
        }

        @Test
        @DisplayName("오늘과 과거의 진행 중 포커스 기록은 페이지네이션을 유지한다")
        void viewFocusRecordByDate_todayAndPastOngoingPreservePagination() {
            User user = UserFixture.user();
            LocalDate today = LocalDate.of(2026, 3, 2);
            LocalDate past = LocalDate.of(2026, 3, 1);
            LocalDateTime serverNow = LocalDateTime.of(2026, 3, 2, 12, 0);
            Book todayBook = BookFixture.book();
            Book pastBook = BookFixture.book();
            Library todayLibrary = LibraryFixture.library(user, todayBook);
            Library pastLibrary = LibraryFixture.library(user, pastBook);
            Focus todayMostRecent = new Focus();
            Focus todayOlder = new Focus();
            Focus pastMostRecent = new Focus();
            Focus pastOlder = new Focus();
            ReflectionTestUtils.setField(todayMostRecent, "id", 60L);
            ReflectionTestUtils.setField(todayMostRecent, "library", todayLibrary);
            ReflectionTestUtils.setField(todayMostRecent, "startedAt", LocalDateTime.of(2026, 3, 2, 10, 0));
            ReflectionTestUtils.setField(todayMostRecent, "endedAt", null);
            ReflectionTestUtils.setField(todayOlder, "id", 59L);
            ReflectionTestUtils.setField(todayOlder, "library", todayLibrary);
            ReflectionTestUtils.setField(todayOlder, "startedAt", LocalDateTime.of(2026, 3, 2, 9, 0));
            ReflectionTestUtils.setField(todayOlder, "endedAt", null);
            ReflectionTestUtils.setField(pastMostRecent, "id", 50L);
            ReflectionTestUtils.setField(pastMostRecent, "library", pastLibrary);
            ReflectionTestUtils.setField(pastMostRecent, "startedAt", LocalDateTime.of(2026, 3, 1, 10, 0));
            ReflectionTestUtils.setField(pastMostRecent, "endedAt", null);
            ReflectionTestUtils.setField(pastOlder, "id", 49L);
            ReflectionTestUtils.setField(pastOlder, "library", pastLibrary);
            ReflectionTestUtils.setField(pastOlder, "startedAt", LocalDateTime.of(2026, 3, 1, 9, 0));
            ReflectionTestUtils.setField(pastOlder, "endedAt", null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user), eq(today), eq(serverNow), isNull(), any(PageRequest.class)
            )).willReturn(new SliceImpl<>(List.of(todayMostRecent, todayOlder), PageRequest.of(0, 2), true));
            given(focusRepository.findByLibraryWithCursorByDate(
                    eq(user), eq(past), eq(serverNow), eq(51L), any(PageRequest.class)
            )).willReturn(new SliceImpl<>(List.of(pastMostRecent, pastOlder), PageRequest.of(0, 2), true));

            CursorResponse<LibraryViewDto.UserBookResponseDto, Long> todayResult =
                    libraryQueryService.getFocusRecordsByDate(1L, today, null, 1);
            CursorResponse<LibraryViewDto.UserBookResponseDto, Long> pastResult =
                    libraryQueryService.getFocusRecordsByDate(1L, past, 51L, 1);

            assertThat(todayResult.items()).singleElement()
                    .extracting(LibraryViewDto.UserBookResponseDto::focusTime)
                    .isEqualTo("02:00:00");
            assertThat(todayResult.nextCursor()).isEqualTo(60L);
            assertThat(todayResult.hasNext()).isTrue();
            assertThat(pastResult.items()).singleElement()
                    .extracting(LibraryViewDto.UserBookResponseDto::focusTime)
                    .isEqualTo("14:00:00");
            assertThat(pastResult.nextCursor()).isEqualTo(50L);
            assertThat(pastResult.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("최근 포커스 조회")
    class ViewRecentFocus {

        @Test
        @DisplayName("최근 포커스가 있으면 bookId/title/page를 반환하고 page 0은 null 처리한다")
        void viewRecentFocus_성공_page_null_처리() {
            User user = UserFixture.user();

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 11L);
            ReflectionTestUtils.setField(book, "title", "최근 도서");
            ReflectionTestUtils.setField(book, "author", "작가");
            ReflectionTestUtils.setField(book, "coverImageKey", "cover");

            Library library = LibraryFixture.library(user, book);
            ReflectionTestUtils.setField(library, "page", 0);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 99L);
            ReflectionTestUtils.setField(focus, "library", library);

            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of(focus));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.RecentFocusResponseDto result = libraryQueryService.getRecentFocus(1L);

            assertThat(result).isNotNull();
            assertThat(result.bookId()).isEqualTo(11L);
            assertThat(result.title()).isEqualTo("최근 도서");
            assertThat(result.page()).isNull();
        }

        @Test
        @DisplayName("최근 포커스가 없으면 null을 반환한다")
        void viewRecentFocus_데이터없음_null반환() {
            User user = UserFixture.user();
            given(focusRepository.findRecentByUser(eq(user), any(PageRequest.class)))
                    .willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.RecentFocusResponseDto result = libraryQueryService.getRecentFocus(1L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("검색 홈용 최근 포커스 목록을 반환한다")
        void viewRecentFocusBooks_성공() {
            User user = UserFixture.user();

            Book book = BookFixture.book();
            ReflectionTestUtils.setField(book, "id", 11L);
            ReflectionTestUtils.setField(book, "title", "최근 도서");
            ReflectionTestUtils.setField(book, "author", "작가");
            ReflectionTestUtils.setField(book, "coverImageKey", "book/users/1/recent-cover.png");

            Library library = LibraryFixture.library(user, book);

            Focus focus = new Focus();
            ReflectionTestUtils.setField(focus, "id", 99L);
            ReflectionTestUtils.setField(focus, "library", library);

            given(focusRepository.findRecentDistinctBooksByUser(eq(user), eq(PageRequest.of(0, 5))))
                    .willReturn(List.of(focus));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/recent-cover.png"))
                    .willReturn("https://cdn.example.com/recent-cover.png");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            List<LibraryViewDto.RecentFocusBookItem> result = libraryQueryService.getRecentFocusBooks(1L, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).bookId()).isEqualTo(11L);
            assertThat(result.get(0).title()).isEqualTo("최근 도서");
            assertThat(result.get(0).author()).isEqualTo("작가");
            assertThat(result.get(0).coverUrl()).isEqualTo("https://cdn.example.com/recent-cover.png");
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/recent-cover.png");
        }
    }

    @Nested
    @DisplayName("읽기 전 도서 5권 조회")
    class ViewBeforeReadingBooks {

        @Test
        @DisplayName("읽기 전 상태 도서 목록을 최대 5건 변환해 반환한다")
        void viewBeforeReadingBooks_성공() {
            User user = UserFixture.user();

            Book firstBook = BookFixture.book();
            ReflectionTestUtils.setField(firstBook, "id", 1L);
            ReflectionTestUtils.setField(firstBook, "isbn13", "1111111111111");
            ReflectionTestUtils.setField(firstBook, "title", "첫 번째");
            ReflectionTestUtils.setField(firstBook, "author", "저자1");
            ReflectionTestUtils.setField(firstBook, "coverImageKey", "book/users/1/first.png");

            Book secondBook = BookFixture.book();
            ReflectionTestUtils.setField(secondBook, "id", 2L);
            ReflectionTestUtils.setField(secondBook, "isbn13", "2222222222222");
            ReflectionTestUtils.setField(secondBook, "title", "두 번째");
            ReflectionTestUtils.setField(secondBook, "author", "저자2");
            ReflectionTestUtils.setField(secondBook, "coverImageKey", "book/users/1/second.png");

            Library firstLibrary = LibraryFixture.library(user, firstBook);
            Library secondLibrary = LibraryFixture.library(user, secondBook);

            given(libraryRepository.findByUserIdAndReadingStatusOrderByIdDesc(
                    1L, ReadingStatus.BEFORE, PageRequest.of(0, 5)))
                    .willReturn(List.of(firstLibrary, secondLibrary));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/first.png"))
                    .willReturn("https://cdn.example.com/first.png");
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/second.png"))
                    .willReturn("https://cdn.example.com/second.png");

            LibraryViewDto.BeforeReadingResponseDto result = libraryQueryService.getBeforeReadingBooks(1L);

            assertThat(result.books()).hasSize(2);
            assertThat(result.books().get(0).coverUrl()).isEqualTo("https://cdn.example.com/first.png");
            assertThat(result.books().get(1).coverUrl()).isEqualTo("https://cdn.example.com/second.png");
        }
    }

    @Nested
    @DisplayName("독서 연도 조회")
    class ViewReadingYears {

        @Test
        @DisplayName("가입 연도부터 현재 연도까지 오름차순으로 반환한다")
        void viewReadingYears_성공() {
            User user = UserFixture.user();
            ReflectionTestUtils.setField(user, "createdDate", LocalDateTime.of(2024, 1, 1, 0, 0));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            LibraryViewDto.YearResponseDto result = libraryQueryService.getReadingYears(user.getId());

            int currentYear = LocalDateTime.now().getYear();
            assertThat(result.years().get(0)).isEqualTo(2024);
            assertThat(result.years().get(result.years().size() - 1)).isEqualTo(currentYear);
            assertThat(result.years()).contains(2025);
        }
    }
}
