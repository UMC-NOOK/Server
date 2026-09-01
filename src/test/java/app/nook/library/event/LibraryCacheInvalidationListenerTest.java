package app.nook.library.event;

import app.nook.redis.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryCacheInvalidationListenerTest {

    private static final long USER_ID = 1L;
    private static final Set<YearMonth> AFFECTED_MONTHS = Set.of(YearMonth.of(2026, 8));

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private LibraryCacheInvalidationListener listener;

    @Test
    void monthlyCacheFailureDoesNotEscapeAndOnboardingEvictionContinues() {
        doThrow(new IllegalStateException("monthly cache failure"))
                .when(redisCacheService).evictLibraryMonthlyCaches(USER_ID, AFFECTED_MONTHS);

        assertThatCode(() -> listener.handleAfterCommit(
                LibraryCacheInvalidateEvent.monthlyAndOnboardingGoal(USER_ID, AFFECTED_MONTHS)))
                .doesNotThrowAnyException();

        verify(redisCacheService).evictOnboardingGoal(USER_ID);
    }

    @Test
    void onboardingCacheFailureDoesNotEscape() {
        doThrow(new IllegalStateException("onboarding cache failure"))
                .when(redisCacheService).evictOnboardingGoal(USER_ID);

        assertThatCode(() -> listener.handleAfterCommit(
                LibraryCacheInvalidateEvent.onboardingGoal(USER_ID)))
                .doesNotThrowAnyException();
    }
}
