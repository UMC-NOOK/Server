package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.FocusTimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryStatsServiceTest {

    @Mock
    private FocusRepository focusRepository;

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

    private FocusRepository.MonthlyFocusStatsProjection toProjection(FocusRankDto.MonthlyFocusRow row) {
        return new FocusRepository.MonthlyFocusStatsProjection() {
            @Override
            public Integer getYearValue() {
                return row.date().getYear();
            }

            @Override
            public Integer getMonthValue() {
                return row.date().getMonthValue();
            }

            @Override
            public Integer getDayValue() {
                return row.date().getDayOfMonth();
            }

            @Override
            public Long getBookId() {
                return row.bookId();
            }

            @Override
            public String getCoverImageUrl() {
                return row.coverImageUrl();
            }

            @Override
            public Long getTotalSec() {
                return row.totalSec();
            }
        };
    }

    private FocusRepository.FocusTimeStatsProjection timeProjection(LocalDate date, Long totalSec) {
        return new FocusRepository.FocusTimeStatsProjection() {
            @Override
            public Integer getYearValue() {
                return date.getYear();
            }

            @Override
            public Integer getMonthValue() {
                return date.getMonthValue();
            }

            @Override
            public Integer getDayValue() {
                return date.getDayOfMonth();
            }

            @Override
            public Long getTotalSec() {
                return totalSec;
            }
        };
    }

    @DisplayName("월별 포커스 통계 조회")
    @Nested
    class ViewMonthly {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("월별 통계를 정상 조회한다")
            void 정상_케이스() {
                Long userId = 1L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

                List<FocusRankDto.MonthlyFocusRow> rows = List.of(
                        row(LocalDate.of(2026, 2, 1), 11L, "cover-11", 1200L),
                        row(LocalDate.of(2026, 2, 2), 22L, "cover-22", 600L),
                        row(LocalDate.of(2026, 2, 3), 33L, "cover-33", 1800L)
                );
                given(focusRepository.findMonthlyFocusStats(userId, start, end))
                        .willReturn(rows.stream().map(LibraryStatsServiceTest.this::toProjection).toList());

                FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

                verify(focusRepository).findMonthlyFocusStats(userId, start, end);
                assertThat(result.yearMonth()).isEqualTo(yearMonth);
                assertThat(result.totalBookCount()).isEqualTo(3);
                assertThat(result.days()).hasSize(3);
            }

            @Test
            @DisplayName("동일 날짜 다중 도서일 때 topBook을 계산한다")
            void 동일_날짜에_여러_책_존재시_topBook_검증() {
                Long userId = 2L;
                YearMonth yearMonth = YearMonth.of(2026, 2);
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

                List<FocusRankDto.MonthlyFocusRow> rows = List.of(
                        row(LocalDate.of(2026, 2, 10), 100L, "cover-100", 300L),
                        row(LocalDate.of(2026, 2, 10), 200L, "cover-200", 1200L),
                        row(LocalDate.of(2026, 2, 11), 300L, "cover-300", 600L)
                );
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
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

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
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

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
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

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
