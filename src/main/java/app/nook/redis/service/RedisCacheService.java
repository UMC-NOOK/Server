package app.nook.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisZSETService redisZSETService;

    // 월별 캐시 정보 무효화
    public void evictLibraryMonthlyCaches(Long userId, Collection<YearMonth> affectedYearMonths) {
        for (YearMonth affectedYearMonth : affectedYearMonths) {
            redisZSETService.evictMonthlyBooks(userId, affectedYearMonth);
            redisZSETService.evictMonthlyFocusTime(userId, affectedYearMonth);
            redisZSETService.evictMonthlyHourlyFocus(userId, affectedYearMonth);
        }
    }
}
