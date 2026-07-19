package app.nook.redis.service;

import app.nook.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisZSETService redisZSETService;
    private final CacheManager cacheManager;

    // 월별 캐시 정보 무효화
    public void evictLibraryMonthlyCaches(Long userId, Collection<YearMonth> affectedYearMonths) {
        for (YearMonth affectedYearMonth : affectedYearMonths) {
            redisZSETService.evictMonthlyBooks(userId, affectedYearMonth);
            redisZSETService.evictMonthlyFocusTime(userId, affectedYearMonth);
            redisZSETService.evictMonthlyHourlyFocus(userId, affectedYearMonth);
        }
    }

    public void evictOnboardingGoal(Long userId) {
        Cache onboardingGoalCache = cacheManager.getCache(CacheConfig.ONBOARDING_GOAL_CACHE);
        if (onboardingGoalCache == null) {
            return;
        }
        onboardingGoalCache.evict(userId);
    }
}
