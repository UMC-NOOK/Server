package app.nook.library.service;

import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.focus.service.FocusDailyTimeCalculator;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.FocusTimeSlot;
import app.nook.r2.service.PresignedUrlService;
import app.nook.redis.service.RedisZSETService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryStatsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime SERVER_NOW = LocalDateTime.of(2026, 2, 15, 0, 10);

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private RedisZSETService redisZSETService;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private Clock clock;

    @Spy
    private FocusDailyTimeCalculator focusDailyTimeCalculator = new FocusDailyTimeCalculator();

    @InjectMocks
    private LibraryStatsService libraryStatsService;

    @BeforeEach
    void setUp() {
        lenient().when(presignedUrlService.resolveImageUrl(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(clock.instant()).thenReturn(SERVER_NOW.atZone(KST).toInstant());
        lenient().when(clock.getZone()).thenReturn(KST);
    }

    @DisplayName("월별 포커스 통계 조회")
    @Nested
    class ViewMonthly {

        @Test
        @DisplayName("완료된 월의 캐시가 있으면 DB를 조회하지 않는다")
        void cacheHit() {
            Long userId = 1L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            FocusRankDto.MonthlyBooksResponseDto cached = new FocusRankDto.MonthlyBooksResponseDto(
                    yearMonth,
                    1,
                    List.of(new FocusRankDto.DailyBookItem(
                            LocalDate.of(2026, 2, 1),
                            1L,
                            new FocusRankDto.BookCalendarInfo(11L, "cover-11")
                    ))
            );

            given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(cached);

            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            assertThat(result).isEqualTo(cached);
            verify(focusRepository, never()).findOverlappingFocusRanges(any(), any(), any());
            verify(redisZSETService, never()).saveMonthlyBooks(any(), any(), any());
            verify(redisZSETService, never()).saveMonthlyBooks(any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("월 범위와 겹치는 세션을 날짜별 도서 통계로 변환한다")
        void aggregateSessionsByDateAndBook() {
            Long userId = 2L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of(
                            focusRange(
                                    LocalDateTime.of(2026, 1, 31, 23, 55),
                                    LocalDateTime.of(2026, 2, 1, 0, 10),
                                    11L,
                                    "cover-11"
                            ),
                            focusRange(
                                    LocalDateTime.of(2026, 2, 2, 10, 0),
                                    LocalDateTime.of(2026, 2, 2, 10, 30),
                                    22L,
                                    "cover-22"
                            )
                    ));

            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            assertThat(result.yearMonth()).isEqualTo(yearMonth);
            assertThat(result.totalBookCount()).isEqualTo(2);
            assertThat(result.days()).extracting(FocusRankDto.DailyBookItem::date)
                    .containsExactlyInAnyOrder(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2));
            verify(redisZSETService).saveMonthlyBooks(eq(userId), eq(yearMonth), eq(2), any());
        }

        @Test
        @DisplayName("같은 날짜 같은 도서의 여러 세션을 합산하고 다른 도서와 분리한다")
        void aggregateSameBookAndSeparateDifferentBooks() {
            Long userId = 3L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of(
                            focusRange(
                                    LocalDateTime.of(2026, 2, 10, 10, 0),
                                    LocalDateTime.of(2026, 2, 10, 10, 5),
                                    100L,
                                    "cover-100"
                            ),
                            focusRange(
                                    LocalDateTime.of(2026, 2, 10, 11, 0),
                                    LocalDateTime.of(2026, 2, 10, 11, 15),
                                    100L,
                                    "cover-100"
                            ),
                            focusRange(
                                    LocalDateTime.of(2026, 2, 10, 12, 0),
                                    LocalDateTime.of(2026, 2, 10, 12, 16),
                                    200L,
                                    "cover-200"
                            )
                    ));

            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            FocusRankDto.DailyBookItem day = result.days().get(0);
            assertThat(day.bookCount()).isEqualTo(2L);
            assertThat(day.topBook().bookId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("캐시 미스면 계산 결과를 Redis ZSET에 저장한다")
        void cacheMiss() {
            Long userId = 4L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of(focusRange(
                            LocalDateTime.of(2026, 2, 1, 10, 0),
                            LocalDateTime.of(2026, 2, 1, 10, 20),
                            11L,
                            "cover-11"
                    )));

            libraryStatsService.viewMonthly(userId, yearMonth);

            verify(focusRepository, times(1)).findOverlappingFocusRanges(userId, rangeStart, rangeEnd);
            verify(redisZSETService, times(1))
                    .saveMonthlyBooks(eq(userId), eq(yearMonth), eq(1), any());
        }

        @Test
        @DisplayName("데이터가 없으면 빈 응답을 캐시에 저장한다")
        void emptyResult() {
            Long userId = 5L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of());

            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            assertThat(result.totalBookCount()).isZero();
            assertThat(result.days()).isEmpty();
            verify(redisZSETService).saveMonthlyBooks(userId, yearMonth, 0, List.of());
        }
    }

    @DisplayName("월별 포커스 시간 통계 조회")
    @Nested
    class ViewFocusTimeStats {

        @Test
        @DisplayName("날짜별 분할 시간을 합산한다")
        void aggregateDailyFocusTime() {
            Long userId = 10L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyFocusTime(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of(
                            focusRange(
                                    LocalDateTime.of(2026, 2, 1, 23, 0),
                                    LocalDateTime.of(2026, 2, 2, 0, 30),
                                    11L,
                                    "cover-11"
                            ),
                            focusRange(
                                    LocalDateTime.of(2026, 2, 2, 1, 0),
                                    LocalDateTime.of(2026, 2, 2, 1, 30),
                                    22L,
                                    "cover-22"
                            )
                    ));

            FocusRankDto.FocusBookResponseDto result = libraryStatsService.viewFocusTimeStats(userId, yearMonth);

            assertThat(result.yearMonth()).isEqualTo(yearMonth);
            assertThat(result.totalFocusMin()).isEqualTo(120);
            assertThat(result.focusBookItems()).hasSize(2);
            assertThat(result.focusBookItems().get(0).timeSlot()).isEqualTo(FocusTimeSlot.FOCUS_04);
            assertThat(result.focusBookItems().get(1).timeSlot()).isEqualTo(FocusTimeSlot.FOCUS_04);
        }

        @Test
        @DisplayName("진행 중 세션과 겹치는 월은 캐시를 읽거나 저장하지 않는다")
        void bypassCacheForInProgressFocus() {
            Long userId = 11L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
            LocalDateTime startedAt = LocalDateTime.of(2026, 2, 14, 23, 55);
            Focus inProgress = Focus.builder()
                    .startedAt(startedAt)
                    .durationSec(0)
                    .build();

            given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(userId))
                    .willReturn(Optional.of(inProgress));
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of(focusRange(startedAt, null, 11L, "cover-11")));

            FocusRankDto.FocusBookResponseDto result = libraryStatsService.viewFocusTimeStats(userId, yearMonth);

            assertThat(result.totalFocusMin()).isEqualTo(15);
            assertThat(result.focusBookItems()).hasSize(2);
            verify(redisZSETService, never()).loadMonthlyFocusTime(any(), any());
            verify(redisZSETService, never()).saveMonthlyFocusTime(any(), any(), any());
        }

        @Test
        @DisplayName("데이터가 없으면 빈 응답을 반환한다")
        void emptyResult() {
            Long userId = 12L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(redisZSETService.loadMonthlyFocusTime(userId, yearMonth)).willReturn(null);
            given(focusRepository.findOverlappingFocusRanges(userId, rangeStart, rangeEnd))
                    .willReturn(List.of());

            FocusRankDto.FocusBookResponseDto result = libraryStatsService.viewFocusTimeStats(userId, yearMonth);

            assertThat(result.totalFocusMin()).isZero();
            assertThat(result.focusBookItems()).isEmpty();
        }
    }

    private FocusRangeStatsDto focusRange(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Long bookId,
            String coverImageKey
    ) {
        return new FocusRangeStatsDto(startedAt, endedAt, bookId, coverImageKey);
    }
}
