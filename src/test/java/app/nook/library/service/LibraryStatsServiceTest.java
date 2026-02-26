package app.nook.library.service;

import app.nook.focus.repository.FocusRepository;
import app.nook.library.dto.FocusRankDto;
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

    @Nested
    class ViewMonthly {

        @Test
        void 정상_케이스() {
            // given
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
                    .willReturn(rows.stream().map(this::toProjection).toList());

            // when
            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            // then
            verify(focusRepository).findMonthlyFocusStats(userId, start, end);
            assertThat(result.yearMonth()).isEqualTo(yearMonth);
            assertThat(result.totalFocusMin()).isEqualTo((1200 + 600 + 1800) / 60);
            assertThat(result.days()).hasSize(3);

            FocusRankDto.DailyBookItem day1 = result.days().get(0);
            assertThat(day1.date()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(day1.bookCount()).isEqualTo(1L);
            assertThat(day1.topBook().bookId()).isEqualTo(11L);
            assertThat(day1.topBook().coverUrl()).isEqualTo("cover-11");

            FocusRankDto.DailyBookItem day2 = result.days().get(1);
            assertThat(day2.date()).isEqualTo(LocalDate.of(2026, 2, 2));
            assertThat(day2.bookCount()).isEqualTo(1L);
            assertThat(day2.topBook().bookId()).isEqualTo(22L);

            FocusRankDto.DailyBookItem day3 = result.days().get(2);
            assertThat(day3.date()).isEqualTo(LocalDate.of(2026, 2, 3));
            assertThat(day3.bookCount()).isEqualTo(1L);
            assertThat(day3.topBook().bookId()).isEqualTo(33L);
        }

        @Test
        void 동일_날짜에_여러_책_존재시_topBook_검증() {
            // given
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
                    .willReturn(rows.stream().map(this::toProjection).toList());

            // when
            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            // then
            verify(focusRepository).findMonthlyFocusStats(userId, start, end);
            FocusRankDto.DailyBookItem sameDay = result.days().stream()
                    .filter(d -> d.date().equals(LocalDate.of(2026, 2, 10)))
                    .findFirst()
                    .orElseThrow();

            assertThat(sameDay.bookCount()).isEqualTo(2L);
            assertThat(sameDay.topBook().bookId()).isEqualTo(200L);
            assertThat(sameDay.topBook().coverUrl()).isEqualTo("cover-200");
        }

        @Test
        void 포커스_기록이_없으면_빈_응답() {
            // given
            Long userId = 3L;
            YearMonth yearMonth = YearMonth.of(2026, 2);
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            given(focusRepository.findMonthlyFocusStats(userId, start, end))
                    .willReturn(List.of());

            // when
            FocusRankDto.MonthlyBooksResponseDto result = libraryStatsService.viewMonthly(userId, yearMonth);

            // then
            verify(focusRepository).findMonthlyFocusStats(userId, start, end);
            assertThat(result.yearMonth()).isEqualTo(yearMonth);
            assertThat(result.totalFocusMin()).isZero();
            assertThat(result.days()).isEmpty();
        }

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
    }
}
