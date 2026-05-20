package app.nook.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisZSETService redisZSETService;

    // 월별 캐시 정보 무효화
    public void evictLibraryMonthlyCaches(Long userId) {
        YearMonth currentYearMonth = YearMonth.now();
        redisZSETService.evictMonthlyBooks(userId, currentYearMonth);
        redisZSETService.evictMonthlyFocusTime(userId, currentYearMonth);
        redisZSETService.evictMonthlyHourlyFocus(userId, currentYearMonth);
    }
}
