package app.nook.redis.service;

import app.nook.library.domain.enums.ReadingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final CacheManager cacheManager;
    private final RedisZSETService redisZSETService;

    // 월별 캐시 정보 무효화
    public void evictLibraryMonthlyCaches(Long userId, List<YearMonth> yearMonths) {
        if (yearMonths == null || yearMonths.isEmpty()) {
            return;
        }

        for (YearMonth yearMonth : yearMonths) {
            redisZSETService.evictMonthlyBooks(userId, yearMonth);
            redisZSETService.evictMonthlyFocusTime(userId, yearMonth);
            redisZSETService.evictMonthlyHourlyFocus(userId, yearMonth);
        }
    }

    // 첫 번째 상태 페이지 무효화
    public void evictLibraryStatusFirstPage(Long userId) {
        Cache statusCache = cacheManager.getCache("libraryStatusFirstPage");
        if (statusCache == null) {
            return;
        }
        for (ReadingStatus status : ReadingStatus.values()) {
            statusCache.evict(userId + ":" + status);
        }
    }
}
