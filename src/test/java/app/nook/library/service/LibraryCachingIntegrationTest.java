package app.nook.library.service;

import app.nook.NookApplication;
import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.book.repository.BookRepository;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.CacheConfig;
import app.nook.global.common.security.WithCustomUser;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.repository.LibraryRepository;
import app.nook.redis.service.RedisZSETService;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(classes = NookApplication.class)
@ActiveProfiles("test")
@WithCustomUser(userId = 1L)
@Import(LibraryCachingIntegrationTest.FixedKstClockConfig.class)
class LibraryCachingIntegrationTest extends AbstractPostgresContainerTests {

    @Autowired
    private LibraryStatsService libraryStatsService;

    @Autowired
    private LibraryCommandService libraryCommandService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private RedisZSETService redisZSETService;

    @MockitoBean
    private FocusRepository focusRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private LibraryRepository libraryRepository;

    @MockitoBean
    private TimelineCommandService timelineCommandService;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private Cache cache;

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

        verify(focusRepository, never()).findOverlappingFocusRanges(any(), any(), any());
    }

    @Test
    void viewMonthly_redisMiss면_db조회후_zset에_저장한다() {
        Long userId = currentUserId();
        YearMonth yearMonth = YearMonth.of(2026, 2);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        given(redisZSETService.loadMonthlyBooks(userId, yearMonth)).willReturn(null);
        given(focusRepository.findOverlappingFocusRanges(eq(userId), eq(start), eq(end)))
                .willReturn(List.of(focusRange(
                        LocalDateTime.of(2026, 2, 1, 10, 0),
                        LocalDateTime.of(2026, 2, 1, 10, 20),
                        10L,
                        "cover-10"
                )));

        libraryStatsService.viewMonthly(userId, yearMonth);

        verify(focusRepository, times(1)).findOverlappingFocusRanges(eq(userId), eq(start), eq(end));
        verify(redisZSETService, times(1)).saveMonthlyBooks(eq(userId), eq(yearMonth), anyInt(), any());
    }

    @Test
    void deleteByBookId_crossMonthEvictsJanuaryAndFebruary() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = book(bookId);

        Library library = library(user, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(focusRepository.findAllByLibraryIdAndLibraryUserId(library.getId(), userId))
                .willReturn(List.of(focus(
                        library,
                        LocalDateTime.of(2026, 1, 31, 23, 30),
                        LocalDateTime.of(2026, 2, 1, 0, 30)
                )));

        libraryCommandService.deleteByBookId(userId, bookId);

        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 1));
        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 2));
        verifyNoMoreInteractions(redisZSETService);
        verify(cacheManager, never()).getCache(CacheConfig.ONBOARDING_GOAL_CACHE);
    }

    @Test
    void deleteByBookId_dailyFocusRowsUnionMonthsAndEvictEachFamilyOnce() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();
        Book book = book(bookId);
        Library library = library(user, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(focusRepository.findAllByLibraryIdAndLibraryUserId(library.getId(), userId))
                .willReturn(List.of(
                        focus(
                                library,
                                LocalDateTime.of(2026, 1, 31, 23, 30),
                                LocalDateTime.of(2026, 2, 1, 0, 0)
                        ),
                        focus(
                                library,
                                LocalDateTime.of(2026, 2, 1, 0, 0),
                                LocalDateTime.of(2026, 2, 1, 0, 30)
                        ),
                        focus(
                                library,
                                LocalDateTime.of(2026, 2, 15, 10, 0),
                                LocalDateTime.of(2026, 2, 15, 10, 30)
                        )
                ));

        libraryCommandService.deleteByBookId(userId, bookId);

        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 1));
        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 2));
        verifyNoMoreInteractions(redisZSETService);
        verify(cacheManager, never()).getCache(CacheConfig.ONBOARDING_GOAL_CACHE);
    }

    @Test
    void deleteByBookId_exactMidnightExcludesNextMonthCacheEviction() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = book(bookId);

        Library library = library(user, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(focusRepository.findAllByLibraryIdAndLibraryUserId(library.getId(), userId))
                .willReturn(List.of(focus(
                        library,
                        LocalDateTime.of(2026, 1, 31, 23, 30),
                        LocalDateTime.of(2026, 2, 1, 0, 0)
                )));

        libraryCommandService.deleteByBookId(userId, bookId);

        YearMonth january = YearMonth.of(2026, 1);
        YearMonth february = YearMonth.of(2026, 2);
        verifyMonthlyCacheEviction(userId, january);
        verify(redisZSETService, never()).evictMonthlyBooks(userId, february);
        verify(redisZSETService, never()).evictMonthlyFocusTime(userId, february);
        verify(redisZSETService, never()).evictMonthlyHourlyFocus(userId, february);
        verifyNoMoreInteractions(redisZSETService);
    }

    @Test
    void deleteByBookId_ongoingUsesKstServerNowForCacheEviction() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = book(bookId);

        Library library = library(user, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(focusRepository.findAllByLibraryIdAndLibraryUserId(library.getId(), userId))
                .willReturn(List.of(focus(
                        library,
                        LocalDateTime.of(2026, 1, 31, 23, 30),
                        null
                )));

        libraryCommandService.deleteByBookId(userId, bookId);

        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 1));
        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 2));
        verifyNoMoreInteractions(redisZSETService);
    }

    @Test
    void deleteByBookId_완독도서_후_월별_zset과_온보딩목표_캐시무효화가_호출된다() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = book(bookId);

        Library library = library(user, book);
        ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.FINISHED);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(focusRepository.findAllByLibraryIdAndLibraryUserId(library.getId(), userId))
                .willReturn(List.of(focus(
                        library,
                        LocalDateTime.of(2026, 2, 1, 10, 0),
                        LocalDateTime.of(2026, 2, 1, 10, 30)
                )));
        given(cacheManager.getCache(CacheConfig.ONBOARDING_GOAL_CACHE)).willReturn(cache);

        libraryCommandService.deleteByBookId(userId, bookId);

        verifyMonthlyCacheEviction(userId, YearMonth.of(2026, 2));
        verify(cache, times(1)).evict(userId);
    }

    @Test
    void cacheInvalidationEvent_evictsOnlyAfterCommitForItsUserAndMonths() {
        Long userId = currentUserId();
        YearMonth january = YearMonth.of(2026, 1);
        YearMonth february = YearMonth.of(2026, 2);
        given(cacheManager.getCache(CacheConfig.ONBOARDING_GOAL_CACHE)).willReturn(cache);
        clearInvocations(cacheManager, cache, redisZSETService);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(LibraryCacheInvalidateEvent.monthlyAndOnboardingGoal(
                    userId,
                    Set.of(january, february)
            ));

            verifyNoInteractions(redisZSETService, cacheManager, cache);
        });

        verifyMonthlyCacheEviction(userId, january);
        verifyMonthlyCacheEviction(userId, february);
        verify(cacheManager, times(1)).getCache(CacheConfig.ONBOARDING_GOAL_CACHE);
        verify(cache, times(1)).evict(userId);
        verifyNoMoreInteractions(redisZSETService, cacheManager, cache);
    }

    @Test
    void cacheInvalidationEvent_doesNotEvictWhenTransactionRollsBack() {
        Long userId = currentUserId();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(LibraryCacheInvalidateEvent.monthlyAndOnboardingGoal(
                    userId,
                    Set.of(YearMonth.of(2026, 2))
            ));
            status.setRollbackOnly();
        });

        verifyNoInteractions(redisZSETService, cacheManager, cache);
    }

    @Test
    void changeReadingStatus_완독_후_온보딩목표_캐시무효화가_호출된다() {
        Long userId = currentUserId();
        Long bookId = 100L;
        User user = currentUser();

        Book book = book(bookId);

        Library library = library(user, book);

        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserIdAndBook(userId, book)).willReturn(Optional.of(library));
        given(cacheManager.getCache(CacheConfig.ONBOARDING_GOAL_CACHE)).willReturn(cache);

        libraryCommandService.changeReadingStatus(
                userId,
                new ReadingStatusRequestDto(bookId, ReadingStatus.FINISHED)
        );

        verify(cache).evict(userId);
    }

    private Book book(Long bookId) {
        Book book = Book.builder()
                .isbn13("9780000000001")
                .title("book")
                .build();
        ReflectionTestUtils.setField(book, "id", bookId);
        return book;
    }

    private Library library(User user, Book book) {
        Library library = new Library(user, book);
        ReflectionTestUtils.setField(library, "id", 999L);
        return library;
    }

    private Focus focus(Library library, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Focus.builder()
                .library(library)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSec(1800)
                .build();
    }

    private void verifyMonthlyCacheEviction(Long userId, YearMonth yearMonth) {
        verify(redisZSETService, times(1)).evictMonthlyBooks(userId, yearMonth);
        verify(redisZSETService, times(1)).evictMonthlyFocusTime(userId, yearMonth);
        verify(redisZSETService, times(1)).evictMonthlyHourlyFocus(userId, yearMonth);
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

    private FocusRangeStatsDto focusRange(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Long bookId,
            String coverImageKey
    ) {
        return new FocusRangeStatsDto(startedAt, endedAt, bookId, coverImageKey);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedKstClockConfig {

        private static final ZoneId KST = ZoneId.of("Asia/Seoul");

        @Bean
        @Primary
        Clock fixedKstClock() {
            return Clock.fixed(Instant.parse("2026-01-31T15:30:00Z"), KST);
        }
    }
}
