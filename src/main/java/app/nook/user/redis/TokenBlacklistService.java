package app.nook.user.redis;

import app.nook.user.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * 로그아웃/회원탈퇴된 access token 블랙리스트
 * <p>
 * JWT는 stateless 라 발급된 access token 을 만료 전 서버가 무효화 불가
 * 로그아웃/탈퇴 시 해당 토큰을 "남은 만료 시간(TTL)" 만큼만 Redis 에 등록,
 * {@link app.nook.user.filter.JwtFilter} 가 인증 세팅 전 조회하여 거부
 * TTL 경과 시 토큰 자연 만료 → Redis 키도 자동 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";
    private static final String VALUE = "logout";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;

    // access token 을 남은 만료 시간만큼 블랙리스트에 등록 (Redis 장애 시 best-effort)
    public void blacklist(String accessToken) {
        long remainingMillis = remainingMillis(accessToken);
        if (remainingMillis <= 0) {
            // 이미 만료된 토큰이면 등록 생략
            return;
        }
        try {
            redisTemplate.opsForValue()
                    .set(PREFIX + accessToken, VALUE, Duration.ofMillis(remainingMillis));
        } catch (RuntimeException e) {
            // 등록 실패해도 토큰은 TTL 후 자연 만료
            log.error("[BLACKLIST] Redis 등록 실패 (best-effort)", e);
        }
    }

    // 블랙리스트 등록 여부 (Redis 장애 시 fail-open)
    public boolean isBlacklisted(String accessToken) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + accessToken));
        } catch (RuntimeException e) {
            // 조회 장애 시 단기 토큰 특성상 가용성 우선 (인증 허용)
            log.error("[BLACKLIST] Redis 조회 실패, fail-open 으로 인증 허용", e);
            return false;
        }
    }

    private long remainingMillis(String accessToken) {
        Date expiration = jwtProvider.parseClaims(accessToken).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
