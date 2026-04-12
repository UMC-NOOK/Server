package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.FocusTimeStatsDto;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.FocusTimeSlot;
import app.nook.redis.service.RedisZSETService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryStatsServiceTest {

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private RedisZSETService redisZSETService;

    @InjectMocks
    private LibraryStatsService libraryStatsService;

    private FocusRankDto.MonthlyFocusRow row(
            LocalDate date,
            Long bookId,
            String coverImageUrl,
            Long totalSec
    ) {
        return new FocusRankDto.MonthlyFocusRow(date, bookId, coverImageUrl, totalSec);
    }

    private MonthlyFocusStatsDto toProjection(FocusRankDto.MonthlyFocusRow row) {
        return new MonthlyFocusStatsDto(
                row.date(),
                row.bookId(),
                row.coverImageUrl(),
                row.totalSec()
        );
    }

    private FocusTimeStatsDto timeProjection(LocalDate date, Long totalSec) {
        return new FocusTimeStatsDto(date, totalSec);
    }

    @DisplayName("월별 포커스 통계 조회")
    @Nested
    class ViewMonthly {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("캐시 히트면 Redis 결과를 반환하고 DB를 조회하지 않는다")
            void 캐시_히트_조회() {
                Long userId = 1L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                FocusRankDto.MonthlyBooksResponseDto cached =
                        new FocusRankDto.MonthlyBooksResponseDto(
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
                verify(focusRepository, never()).findMonthlyFocusStats(any(), any(), any());
                verify(redisZSETService, never()).saveMonthlyBooks(any(), any(), any());
                verify(redisZSETService, never()).saveMonthlyBooks(any(), any(), anyInt(), any());
            }

            @Test
            @DisplayName("월별 통계를 정상 조회한다")
            void 정상_케이스() {
                Long userId = 1L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);

                List<FocusRankDto.MonthlyFocusRow> rows = List.of(
                        row(LocalDate.of(2026, 2, 1), 11L, "cover-11", 1200L),
                        row(LocalDate.of(2026, 2, 2), 22L, "cover-22", 600L),
                        row(LocalDate.of(2026, 2, 3), 33L, "cover-33", 1800L)
                );
                given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
                given(focusRepository.findMonthlyFocusStats(userId, start, end))
                        .willReturn(rows.stream().map(LibraryStatsServiceTest.this::toProjection).toList());

                FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

                verify(focusRepository).findMonthlyFocusStats(userId, start, end);
                assertThat(result.yearMonth()).isEqualTo(yearMonth);
                assertThat(result.totalBookCount()).isEqualTo(3);
                assertThat(result.days()).hasSize(3);
            }

            @Test
            @DisplayName("캐시 미스면 DB 조회 후 Redis ZSET에 저장한다")
            void 캐시_미스_조회() {
                Long userId = 4L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);
                List<FocusRankDto.MonthlyFocusRow> rows = List.of(
                        row(LocalDate.of(2026, 2, 1), 11L, "cover-11", 1200L)
                );

                given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
                given(focusRepository.findMonthlyFocusStats(userId, start, end))
                        .willReturn(rows.stream().map(LibraryStatsServiceTest.this::toProjection).toList());

                libraryStatsService.viewMonthly(userId, yearMonth);

                verify(focusRepository, times(1)).findMonthlyFocusStats(userId, start, end);
                verify(redisZSETService, times(1)).saveMonthlyBooks(eq(userId), eq(yearMonth), anyInt(), any());
            }

            @Test
            @DisplayName("동일 날짜 다중 도서일 때 topBook을 계산한다")
            void 동일_날짜에_여러_책_존재시_topBook_검증() {
                Long userId = 2L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);

                List<FocusRankDto.MonthlyFocusRow> rows = List.of(
                        row(LocalDate.of(2026, 2, 10), 100L, "cover-100", 300L),
                        row(LocalDate.of(2026, 2, 10), 200L, "cover-200", 1200L),
                        row(LocalDate.of(2026, 2, 11), 300L, "cover-300", 600L)
                );
                given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
                given(focusRepository.findMonthlyFocusStats(userId, start, end))
                        .willReturn(rows.stream().map(LibraryStatsServiceTest.this::toProjection).toList());

                FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

                verify(focusRepository).findMonthlyFocusStats(userId, start, end);
                FocusRankDto.DailyBookItem sameDay = result.days().stream()
                        .filter(d -> d.date().equals(LocalDate.of(2026, 2, 10)))
                        .findFirst()
                        .orElseThrow();
                assertThat(sameDay.bookCount()).isEqualTo(2L);
                assertThat(sameDay.topBook().bookId()).isEqualTo(200L);
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("데이터가 없으면 빈 응답을 반환한다")
            void 포커스_기록이_없으면_빈_응답() {
                Long userId = 3L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);

                given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
                given(focusRepository.findMonthlyFocusStats(userId, start, end))
                        .willReturn(List.of());

                FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

                verify(focusRepository).findMonthlyFocusStats(userId, start, end);
                assertThat(result.yearMonth()).isEqualTo(yearMonth);
                assertThat(result.totalBookCount()).isZero();
                assertThat(result.days()).isEmpty();
            }
        }
    }

    @DisplayName("월별 포커스 시간 통계 조회")
    @Nested
    class ViewFocusTimeStats {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("월별 포커스 시간 통계를 정상 조회한다")
            void 정상_조회() {
                Long userId = 10L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);

                given(redisZSETService.loadMonthlyFocusTime(userId, yearMonth)).willReturn(null);
                given(focusRepository.findFocusTimeStats(userId, start, end))
                        .willReturn(List.of(
                                timeProjection(LocalDate.of(2026, 2, 1), 3600L),
                                timeProjection(LocalDate.of(2026, 2, 2), 1800L)
                        ));

                FocusRankDto.FocusBookResponseDto result = libraryStatsService.viewFocusTimeStats(userId, yearMonth);

                verify(focusRepository).findFocusTimeStats(userId, start, end);
                assertThat(result.yearMonth()).isEqualTo(yearMonth);
                assertThat(result.totalFocusMin()).isEqualTo(90);
                assertThat(result.focusBookItems()).hasSize(2);
                assertThat(result.focusBookItems().get(0).timeSlot()).isEqualTo(FocusTimeSlot.FOCUS_04);
                assertThat(result.focusBookItems().get(1).timeSlot()).isEqualTo(FocusTimeSlot.FOCUS_02);
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("데이터가 없으면 빈 응답을 반환한다")
            void 데이터_없음() {
                Long userId = 11L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.plusMonths(1).atDay(1);

                given(redisZSETService.loadMonthlyFocusTime(userId, yearMonth)).willReturn(null);
                given(focusRepository.findFocusTimeStats(userId, start, end))
                        .willReturn(List.of());

                FocusRankDto.FocusBookResponseDto result = libraryStatsService.viewFocusTimeStats(userId, yearMonth);

                verify(focusRepository).findFocusTimeStats(userId, start, end);
                assertThat(result.totalFocusMin()).isZero();
                assertThat(result.focusBookItems()).isEmpty();
            }
        }
    }
}
