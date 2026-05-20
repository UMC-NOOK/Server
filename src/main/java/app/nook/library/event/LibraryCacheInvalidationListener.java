package app.nook.library.event;

import app.nook.redis.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋 후에 캐시 무효화 실행하는 메서드
 */
@Component
@RequiredArgsConstructor
public class LibraryCacheInvalidationListener {

    private final RedisCacheService redisCacheService;

    // 트랜잭션 커밋 후 이벤트 처리하여 캐시 무효화
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(LibraryCacheInvalidateEvent event) {
        redisCacheService.evictLibraryMonthlyCaches(event.userId());
    }
}
