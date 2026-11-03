package app.nook.yes24.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.global.exception.RateLimitExceededException;
import app.nook.yes24.exception.Yes24ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static app.nook.yes24.service.Yes24QuotaService.REMAINING_DAY_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class Yes24QuotaServiceTest {

    // 2026-11-03 14:49:18 KST. 다음 KST 자정(reset-day)까지 33042초
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1793684958L), ZoneId.of("Asia/Seoul"));
    private static final String RESET_DAY = "1793718000";

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOps;
    private Yes24QuotaService quotaService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        quotaService = new Yes24QuotaService(redisTemplate, new ConcurrentMapCacheManager(), CLOCK, 4000);
    }

    @Test
    @DisplayName("남은 일일 호출 수를 reset-day(KST 자정)까지 기록")
    void record_savesUntilResetDay() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-remaining-day", "19995");
        headers.add("x-ratelimit-reset-day", RESET_DAY);

        quotaService.record(headers);

        verify(valueOps).set(REMAINING_DAY_KEY, "19995", Duration.ofSeconds(33042));
    }

    @Test
    @DisplayName("한도 헤더가 없으면 기록하지 않음")
    void record_noHeaders() {
        quotaService.record(new HttpHeaders());

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 장애 시 기록은 무시하고 정상 단계로 판단")
    void redisFailure_failOpen() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-remaining-day", "10");
        headers.add("x-ratelimit-reset-day", RESET_DAY);
        willThrow(new RedisConnectionFailureException("down")).given(valueOps).set(anyString(), anyString(), any(Duration.class));
        given(valueOps.get(REMAINING_DAY_KEY)).willThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> quotaService.record(headers)).doesNotThrowAnyException();
        assertThat(quotaService.isLow()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {"4000, true", "0, true", "4001, false", "NULL, false"}, nullValues = "NULL")
    @DisplayName("남은 호출 수가 기준값 이하이면 부족 단계 (값이 없으면 정상)")
    void isLow(String remaining, boolean low) {
        given(valueOps.get(REMAINING_DAY_KEY)).willReturn(remaining);

        assertThat(quotaService.isLow()).isEqualTo(low);
    }

    @Test
    @DisplayName("429 - 남은 일일 호출이 0이면 하루 한도 문구, 남은 시간은 기록 키의 TTL")
    void rateLimited_daily() {
        given(valueOps.get(REMAINING_DAY_KEY)).willReturn("0");
        given(redisTemplate.getExpire(REMAINING_DAY_KEY)).willReturn(33042L);

        RateLimitExceededException e = quotaService.rateLimited(new HttpHeaders());

        assertThat(e.getErrorCode()).isEqualTo(Yes24ErrorCode.BOOK_API_RATE_LIMITED);
        assertThat(e.getMessage()).isEqualTo("도서 정보 한도에 도달했습니다. 내일 다시 시도해 주세요.");
        assertThat(e.getRetryAfterSeconds()).isEqualTo(33042);
    }

    @Test
    @DisplayName("429 - 초당 한도는 reset-second(Unix 시각)까지 남은 초로 안내")
    void rateLimited_perSecond() {
        given(valueOps.get(REMAINING_DAY_KEY)).willReturn("15000");
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-reset-second", "1793684961");

        RateLimitExceededException e = quotaService.rateLimited(headers);

        assertThat(e.getMessage()).isEqualTo("도서 정보 요청이 너무 많습니다. 3초 후 다시 시도해 주세요.");
        assertThat(e.getRetryAfterSeconds()).isEqualTo(3);
    }

    @Test
    @DisplayName("429 - 남은 호출 수와 reset-second를 모르면 초당 한도 1초로 안내")
    void rateLimited_unknown() {
        RateLimitExceededException e = quotaService.rateLimited(null);

        assertThat(e.getMessage()).isEqualTo("도서 정보 요청이 너무 많습니다. 1초 후 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("last-good은 카테고리별로 저장하고 그대로 읽음")
    void lastGood_saveAndFind() {
        List<BookResponseDto.BookPreviewDto> books = List.of(
                new BookResponseDto.BookPreviewDto("9788936434267", "소년이 온다", "한강", "https://example.com/c.jpg", "창비", 1));

        quotaService.saveLastGood("001001", books);

        assertThat(quotaService.findLastGood("001001")).contains(books);
        assertThat(quotaService.findLastGood("001002")).isEmpty();
    }
}
