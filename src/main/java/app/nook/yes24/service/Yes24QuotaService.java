package app.nook.yes24.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.global.config.CacheConfig;
import app.nook.global.exception.RateLimitExceededException;
import app.nook.yes24.exception.Yes24ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * YES24 API 키 한도(모든 사용자 공유) 상태 관리
 * - 응답 헤더의 남은 일일 호출 수를 Redis에 기록하고, 부족 단계 여부를 판단한다.
 * - 베스트셀러 마지막 성공 목록(last-good)을 보관한다.
 * - Redis 오류는 무시하고 정상 단계로 본다(fail-open). 값은 인스턴스·동시 요청 간 근사값이다.
 */
@Slf4j
@Service
public class Yes24QuotaService {

    static final String REMAINING_DAY_KEY = "yes24:quota:remaining-day";

    private static final String REMAINING_DAY_HEADER = "x-ratelimit-remaining-day";
    private static final String RESET_DAY_HEADER = "x-ratelimit-reset-day";
    private static final String RESET_SECOND_HEADER = "x-ratelimit-reset-second";

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheManager cacheManager;
    private final Clock clock;
    private final long lowThreshold;

    public Yes24QuotaService(RedisTemplate<String, String> redisTemplate,
                             CacheManager cacheManager,
                             Clock clock,
                             @Value("${yes24.quota.low-threshold:4000}") long lowThreshold) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
        this.clock = clock;
        this.lowThreshold = lowThreshold;
    }

    /** 응답 헤더의 남은 일일 호출 수를 reset-day(Unix 초, KST 자정)까지 저장. 헤더가 없으면 기록하지 않는다 */
    public void record(HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        String remaining = headers.getFirst(REMAINING_DAY_HEADER);
        String resetDay = headers.getFirst(RESET_DAY_HEADER);
        if (remaining == null || resetDay == null) {
            return;
        }
        try {
            long ttlSeconds = Long.parseLong(resetDay.trim()) - clock.instant().getEpochSecond();
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(REMAINING_DAY_KEY, remaining.trim(), Duration.ofSeconds(ttlSeconds));
            }
        } catch (RuntimeException e) {
            log.warn("[YES24_QUOTA_RECORD_FAIL] message='{}'", e.getMessage());
        }
    }

    /** 남은 일일 호출 수가 기준값 이하이면 부족 단계 (값이 없거나 Redis 오류면 정상) */
    public boolean isLow() {
        return remainingDay().map(remaining -> remaining <= lowThreshold).orElse(false);
    }

    /** YES24 429 → BOOK_API-003. 남은 일일 호출이 0 이하면 하루 한도, 아니면 초당 한도 */
    public RateLimitExceededException rateLimited(HttpHeaders headers) {
        record(headers);
        if (remainingDay().map(remaining -> remaining <= 0).orElse(false)) {
            return new RateLimitExceededException(Yes24ErrorCode.BOOK_API_RATE_LIMITED,
                    "도서 정보 한도에 도달했습니다.", "내일 다시 시도해 주세요.", secondsUntilDayReset());
        }
        return RateLimitExceededException.retryAfter(Yes24ErrorCode.BOOK_API_RATE_LIMITED,
                "도서 정보 요청이 너무 많습니다.", secondsUntilSecondReset(headers));
    }

    @SuppressWarnings("unchecked")
    public Optional<List<BookResponseDto.BookPreviewDto>> findLastGood(String categoryId) {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.LAST_GOOD_BESTSELLERS_CACHE);
            return cache == null ? Optional.empty() : Optional.ofNullable(cache.get(categoryId, List.class));
        } catch (RuntimeException e) {
            log.warn("[YES24_LAST_GOOD_READ_FAIL] categoryId={}, message='{}'", categoryId, e.getMessage());
            return Optional.empty();
        }
    }

    public void saveLastGood(String categoryId, List<BookResponseDto.BookPreviewDto> books) {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.LAST_GOOD_BESTSELLERS_CACHE);
            if (cache != null) {
                cache.put(categoryId, books);
            }
        } catch (RuntimeException e) {
            log.warn("[YES24_LAST_GOOD_SAVE_FAIL] categoryId={}, message='{}'", categoryId, e.getMessage());
        }
    }

    private Optional<Long> remainingDay() {
        try {
            String value = redisTemplate.opsForValue().get(REMAINING_DAY_KEY);
            return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
        } catch (RuntimeException e) {
            log.warn("[YES24_QUOTA_READ_FAIL] message='{}'", e.getMessage());
            return Optional.empty();
        }
    }

    // 남은 호출 키는 YES24 reset-day(KST 자정)에 만료되므로 그 TTL이 곧 하루 한도 초기화까지 남은 시간
    private long secondsUntilDayReset() {
        try {
            Long ttl = redisTemplate.getExpire(REMAINING_DAY_KEY);
            return ttl == null || ttl <= 0 ? 1 : ttl;
        } catch (RuntimeException e) {
            return 1;
        }
    }

    // reset-second는 대기 초가 아니라 Unix 시각(초)이다. 없거나 해석할 수 없으면 1초
    private long secondsUntilSecondReset(HttpHeaders headers) {
        String resetSecond = headers == null ? null : headers.getFirst(RESET_SECOND_HEADER);
        try {
            return resetSecond == null ? 1 : Math.max(1, Long.parseLong(resetSecond.trim()) - clock.instant().getEpochSecond());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
