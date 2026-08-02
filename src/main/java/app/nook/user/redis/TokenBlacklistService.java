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

    /** access token 을 남은 만료 시간만큼 블랙리스트에 등록 */
    public void blacklist(String accessToken) {
        long remainingMillis = remainingMillis(accessToken);
        if (remainingMillis <= 0) {
            // 이미 만료된 토큰이면 등록할 필요 없음
            return;
        }
        redisTemplate.opsForValue()
                .set(PREFIX + accessToken, VALUE, Duration.ofMillis(remainingMillis));
    }

    /** 블랙리스트 등록 여부 */
    public boolean isBlacklisted(String accessToken) {
        return redisTemplate.hasKey(PREFIX + accessToken);
    }

    private long remainingMillis(String accessToken) {
        Date expiration = jwtProvider.parseClaims(accessToken).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
