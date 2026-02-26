package app.nook.library.service;

import app.nook.NookApplication;
import app.nook.book.domain.Book;
import app.nook.book.repository.BookRepository;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.common.security.WithCustomUser;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {NookApplication.class, LibraryCachingIntegrationTest.CacheTestConfig.class},
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("test")
@WithCustomUser(userId = 1L)
class LibraryCachingIntegrationTest {

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "libraryMonthlyCurrent",
                    "focusMonthlyCurrent",
                    "libraryStatusFirstPage"
            );
        }
    }

    @Autowired
    private LibraryStatsService libraryStatsService;

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private FocusRepository focusRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private LibraryRepository libraryRepository;

    @MockitoBean
    private BookTimeLineRepository bookTimeLineRepository;

    @BeforeEach
    void clearCaches() {
        clearCache("libraryMonthlyCurrent");
        clearCache("focusMonthlyCurrent");
        clearCache("libraryStatusFirstPage");
    }

    @Test
    void viewMonthly_동일키_호출시_한번만_조회된다() {
        // given
        Long userId = currentUserId();
        YearMonth yearMonth = YearMonth.of(2026, 2);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        given(focusRepository.findMonthlyFocusStats(eq(userId), eq(start), eq(end)))
                .willReturn(List.of(monthlyProjection(LocalDate.of(2026, 2, 1), 10L, "cover-10", 1200L)));

        // when
        libraryStatsService.viewMonthly(userId, yearMonth);
        libraryStatsService.viewMonthly(userId, yearMonth);

        // then
        verify(focusRepository, times(1)).findMonthlyFocusStats(eq(userId), eq(start), eq(end));
    }

    @Test
    void deleteById_후_viewMonthly_재조회시_캐시가_무효화된다() {
        // given
        Long userId = currentUserId();
        Long bookId = 100L;
        YearMonth yearMonth = YearMonth.of(2026, 2);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        User user = currentUser();

        Book book = Book.builder()
                .isbn13("9780000000001")
                .title("book")
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);

        Library library = new Library(user, book);
        ReflectionTestUtils.setField(library, "id", 999L);

        given(focusRepository.findMonthlyFocusStats(eq(userId), eq(start), eq(end)))
                .willReturn(List.of(monthlyProjection(LocalDate.of(2026, 2, 1), 10L, "cover-10", 1200L)));

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
        given(focusRepository.findDistinctFocusYearMonthsByLibraryAndUser(library.getId(), userId))
                .willReturn(List.of(yearMonthProjection(2026, 2)));

        // when
        libraryStatsService.viewMonthly(userId, yearMonth); // cache put
        libraryService.deleteById(user, bookId); // evict
        libraryStatsService.viewMonthly(userId, yearMonth); // re-query

        // then
        verify(focusRepository, times(2)).findMonthlyFocusStats(eq(userId), eq(start), eq(end));
    }

    @Test
    void viewMonthly_키가_다르면_각각_캐시가_분리된다() {
        // given
        Long user1 = currentUserId();
        Long user2 = 2L;
        YearMonth feb = YearMonth.of(2026, 2);
        YearMonth mar = YearMonth.of(2026, 3);

        LocalDateTime febStart = feb.atDay(1).atStartOfDay();
        LocalDateTime febEnd = feb.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime marStart = mar.atDay(1).atStartOfDay();
        LocalDateTime marEnd = mar.plusMonths(1).atDay(1).atStartOfDay();

        given(focusRepository.findMonthlyFocusStats(any(), any(), any()))
                .willReturn(List.of(monthlyProjection(LocalDate.of(2026, 2, 1), 10L, "cover-10", 1200L)));

        // when
        libraryStatsService.viewMonthly(user1, feb);
        libraryStatsService.viewMonthly(user1, mar);
        libraryStatsService.viewMonthly(user2, feb);
        libraryStatsService.viewMonthly(user1, feb); // hit

        // then
        verify(focusRepository, times(1)).findMonthlyFocusStats(eq(user1), eq(febStart), eq(febEnd));
        verify(focusRepository, times(1)).findMonthlyFocusStats(eq(user1), eq(marStart), eq(marEnd));
        verify(focusRepository, times(1)).findMonthlyFocusStats(eq(user2), eq(febStart), eq(febEnd));
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
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

    private FocusRepository.MonthlyFocusStatsProjection monthlyProjection(
            LocalDate dateValue,
            Long bookId,
            String coverImageUrl,
            Long totalSec
    ) {
        return new FocusRepository.MonthlyFocusStatsProjection() {
            @Override
            public Integer getYearValue() {
                return dateValue.getYear();
            }

            @Override
            public Integer getMonthValue() {
                return dateValue.getMonthValue();
            }

            @Override
            public Integer getDayValue() {
                return dateValue.getDayOfMonth();
            }

            @Override
            public Long getBookId() {
                return bookId;
            }

            @Override
            public String getCoverImageUrl() {
                return coverImageUrl;
            }

            @Override
            public Long getTotalSec() {
                return totalSec;
            }
        };
    }

    private FocusRepository.FocusYearMonthProjection yearMonthProjection(Integer year, Integer month) {
        return new FocusRepository.FocusYearMonthProjection() {
            @Override
            public Integer getYearValue() {
                return year;
            }

            @Override
            public Integer getMonthValue() {
                return month;
            }
        };
    }
}
