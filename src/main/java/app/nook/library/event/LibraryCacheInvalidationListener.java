package app.nook.library.event;

import app.nook.library.domain.enums.ReadingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LibraryCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(LibraryCacheInvalidateEvent event) {
        if (event.affectedYearMonths() != null && !event.affectedYearMonths().isEmpty()) {
            Cache monthlyCache = cacheManager.getCache("libraryMonthlyCurrent");
            Cache focusTimeCache = cacheManager.getCache("focusMonthlyCurrent");
            for (var yearMonth : event.affectedYearMonths()) {
                String key = event.userId() + ":" + yearMonth;
                if (monthlyCache != null) {
                    monthlyCache.evict(key);
                }
                if (focusTimeCache != null) {
                    focusTimeCache.evict(key);
                }
            }
        }

        if (event.evictStatusFirstPage()) {
            Cache statusCache = cacheManager.getCache("libraryStatusFirstPage");
            if (statusCache == null) {
                return;
            }
            for (ReadingStatus status : ReadingStatus.values()) {
                statusCache.evict(event.userId() + ":" + status);
            }
        }
    }
}
