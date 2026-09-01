package app.nook.library.event;

import app.nook.redis.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋 후에 캐시 무효화 실행하는 메서드
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryCacheInvalidationListener {

    private final RedisCacheService redisCacheService;

    // 트랜잭션 커밋 후 이벤트 처리하여 캐시 무효화
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(LibraryCacheInvalidateEvent event) {
        try {
            redisCacheService.evictLibraryMonthlyCaches(event.userId(), event.affectedYearMonths());
        } catch (RuntimeException exception) {
            log.warn("[LIBRARY_CACHE] 월별 캐시 무효화 실패 userId={}, affectedYearMonths={}",
                    event.userId(), event.affectedYearMonths(), exception);
        }

        if (event.evictOnboardingGoal()) {
            try {
                redisCacheService.evictOnboardingGoal(event.userId());
            } catch (RuntimeException exception) {
                log.warn("[LIBRARY_CACHE] 온보딩 목표 캐시 무효화 실패 userId={}",
                        event.userId(), exception);
            }
        }
    }
}
