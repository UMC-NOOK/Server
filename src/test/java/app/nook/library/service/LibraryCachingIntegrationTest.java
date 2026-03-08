package app.nook.library.service;

import app.nook.NookApplication;
import app.nook.book.domain.Book;
import app.nook.book.repository.BookRepository;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
import app.nook.global.common.security.WithCustomUser;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.redis.service.RedisZSETService;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = NookApplication.class)
@ActiveProfiles("test")
@WithCustomUser(userId = 1L)
class LibraryCachingIntegrationTest {

    @Autowired
    private LibraryStatsService libraryStatsService;

    @Autowired
    private LibraryService libraryService;

    @MockitoBean
    private RedisZSETService redisZSETService;

    @MockitoBean
    private FocusRepository focusRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private LibraryRepository libraryRepository;

    @MockitoBean
    private BookTimeLineRepository bookTimeLineRepository;

    @Test
    void viewMonthly_redisHit이면_db를_조회하지_않는다() {
        Long userId = currentUserId();
        YearMonth yearMonth = YearMonth.of(2026, 2);

        given(redisZSETService.loadMonthlyBooks(userId, yearMonth))
                .willReturn(new app.nook.library.dto.FocusRankDto.MonthlyBooksResponseDto(
                        yearMonth,
                        1,
                        List.of(new app.nook.library.dto.FocusRankDto.DailyBookItem(
                                LocalDate.of(2026, 2, 1),
                                1L,
                                new app.nook.library.dto.FocusRankDto.BookCalendarInfo(10L, "cover-10")
                        ))
                ));

        libraryStatsService.viewMonthly(userId, yearMonth);

        verify(focusRepository, never()).findMonthlyFocusStats(any(), any(), any());
    }

    @Test
    void viewMonthly_redisMiss면_db조회후_zset에_저장한다() {
        Long userId = currentUserId();
        YearMonth yearMonth = YearMonth.of(2026, 2);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.plusMonths(1).atDay(1);

        given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
        given(focusRepository.findMonthlyFocusStats(eq(userId), eq(start), eq(end)))
                .willReturn(List.of(monthlyProjection(LocalDate.of(2026, 2, 1), 10L, "cover-10", 1200L)));

        libraryStatsService.viewMonthly(userId, yearMonth);

        verify(focusRepository, times(1)).findMonthlyFocusStats(eq(userId), eq(start), eq(end));
        verify(redisZSETService, times(1)).saveMonthlyBooks(eq(userId), eq(yearMonth), any());
    }

    @Test
    void deleteById_후_월별_zset_무효화가_호출된다() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = Book.builder()
                .isbn13("9780000000001")
                .title("book")
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);

        Library library = new Library(user, book);
        ReflectionTestUtils.setField(library, "id", 999L);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
        given(focusRepository.findDistinctFocusDatesByLibraryAndUser(library.getId(), userId))
                .willReturn(List.of(LocalDate.of(2026, 2, 1)));

        libraryService.deleteById(user, bookId);

        verify(redisZSETService, times(1)).evictMonthlyBooks(userId, YearMonth.of(2026, 2));
        verify(redisZSETService, times(1)).evictMonthlyFocusTime(userId, YearMonth.of(2026, 2));
        verify(redisZSETService, times(1)).evictMonthlyHourlyFocus(userId, YearMonth.of(2026, 2));
    }

    private Long currentUserId() {
        return currentUser().getId();
    }

    private User currentUser() {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUser();
    }

    private MonthlyFocusStatsDto monthlyProjection(
            LocalDate dateValue,
            Long bookId,
            String coverImageUrl,
            Long totalSec
    ) {
        return new MonthlyFocusStatsDto(dateValue, bookId, coverImageUrl, totalSec);
    }
}
