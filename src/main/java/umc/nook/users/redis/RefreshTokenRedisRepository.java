package umc.nook.users.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.users.domain.RefreshToken;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "jwtToken:";                     // 본문 저장 키
    private static final String REFRESH_TOKEN_INDEX = "jwtToken:refreshToken:"; // refreshToken -> tokenId
    private static final String USER_ID_INDEX = "jwtToken:userId:";       // userId -> tokenId

    private static final long MAX_TTL_SECONDS = 60 * 60 * 24 * 3; // 3일

    /** 저장: 필수값 검증 + 만료 검증 + TTL(만료시각과 동기화) */
    public void save(RefreshToken refreshToken) {
        validateForSave(refreshToken);

        long ttl = ttlFromExpiration(refreshToken.getExpiration());
        if (ttl <= 0) throw new RefreshTokenExpiredException();

        String mainKey = PREFIX + refreshToken.getTokenId();

        // 본문 저장
        redisTemplate.opsForValue().set(mainKey, refreshToken, ttl, TimeUnit.SECONDS);

        // 인덱스(동일 TTL 유지)
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_INDEX + refreshToken.getRefreshToken(),
                refreshToken.getTokenId(),
                ttl, TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                USER_ID_INDEX + refreshToken.getUserId(),
                refreshToken.getTokenId(),
                ttl, TimeUnit.SECONDS
        );
    }

    /** tokenId로 조회 (만료 시 예외 및 정리) */
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) return Optional.empty();

        Object value = redisTemplate.opsForValue().get(PREFIX + tokenId);
        if (value == null) return Optional.empty();

        RefreshToken token = convert(value);
        if (isExpired(token)) {
            deleteByTokenId(tokenId);
            throw new RefreshTokenExpiredException();
        }
        return Optional.of(token);
    }

    /** refreshToken으로 조회 (만료 시 예외) */
    public Optional<RefreshToken> findByRefreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) return Optional.empty();

        String tokenId = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_INDEX + refreshTokenValue);
        if (tokenId == null) return Optional.empty();

        return findByTokenId(tokenId);
    }

    /** userId로 조회 (만료 시 예외) */
    public Optional<RefreshToken> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();

        String tokenId = (String) redisTemplate.opsForValue().get(USER_ID_INDEX + userId);
        if (tokenId == null) return Optional.empty();

        return findByTokenId(tokenId);
    }

    /** 없으면/만료면 예외를 던지는 강제 조회(필요 시 사용) */
    public RefreshToken getRequiredByRefreshToken(String refreshTokenValue) {
        return findByRefreshToken(refreshTokenValue)
                .orElseThrow(RefreshTokenNotFoundException::new);
    }

    public void deleteByTokenId(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) return;
        // 인덱스 정리
        findByTokenId(tokenId).ifPresent(token -> {
            redisTemplate.delete(REFRESH_TOKEN_INDEX + token.getRefreshToken());
            redisTemplate.delete(USER_ID_INDEX + token.getUserId());
        });
        // 본문 삭제
        redisTemplate.delete(PREFIX + tokenId);
    }

    public void deleteByRefreshToken(String refreshTokenValue) {
        findByRefreshToken(refreshTokenValue).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }

    public void deleteByUserId(Long userId) {
        findByUserId(userId).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }
    private void validateForSave(RefreshToken token) {
        // 객체 자체 검증
        if (token == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // tokenId
        if (!StringUtils.hasText(token.getTokenId())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // refreshToken(원문 토큰 문자열)
        if (!StringUtils.hasText(token.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // userId
        if (token.getUserId() == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 만료시각
        if (token.getExpiration() == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 만료 여부
        if (token.getExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }
    private RefreshToken convert(Object value) {
        try {
            return objectMapper.convertValue(value, RefreshToken.class);
        } catch (IllegalArgumentException e) {
            throw new RefreshTokenInvalidException("토큰 역직렬화에 실패했습니다.");
        }
    }

    private boolean isExpired(RefreshToken token) {
        return token == null || token.getExpiration() == null
                || token.getExpiration().isBefore(LocalDateTime.now());
    }

    /** 만료시각까지 남은 초(상한 적용) */
    private long ttlFromExpiration(LocalDateTime expiration) {
        long seconds = Duration.between(LocalDateTime.now(), expiration).getSeconds();
        if (seconds <= 0) return 0;
        return Math.min(seconds, MAX_TTL_SECONDS);
    }

    public static class RefreshTokenNotFoundException extends RuntimeException {
        public RefreshTokenNotFoundException() { super("리프레시 토큰을 찾을 수 없습니다."); }
    }
    public static class RefreshTokenExpiredException extends RuntimeException {
        public RefreshTokenExpiredException() { super("리프레시 토큰이 만료되었습니다."); }
    }
    public static class RefreshTokenInvalidException extends RuntimeException {
        public RefreshTokenInvalidException(String msg) { super(msg); }
    }
}
