package umc.nook.users.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import umc.nook.users.domain.RefreshToken;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String PREFIX = "jwtToken:"; // 본문 저장 키
    private static final String REFRESH_TOKEN_INDEX = "jwtToken:refreshToken:"; // refreshToken 인덱스
    private static final String USER_ID_INDEX = "jwtToken:userId:"; // userId 인덱스
    private static final long TTL_SECONDS = 60 * 60 * 24 * 3; // 3일

    public void save(RefreshToken refreshToken) {
        String mainKey = PREFIX + refreshToken.getTokenId();

        // 본문 저장
        redisTemplate.opsForValue().set(mainKey, refreshToken, TTL_SECONDS, TimeUnit.SECONDS);

        // 인덱스 키 저장 → O(1) 조회 가능
        redisTemplate.opsForValue().set(REFRESH_TOKEN_INDEX + refreshToken.getRefreshToken(),
                refreshToken.getTokenId(), TTL_SECONDS, TimeUnit.SECONDS);

        redisTemplate.opsForValue().set(USER_ID_INDEX + refreshToken.getUserId(),
                refreshToken.getTokenId(), TTL_SECONDS, TimeUnit.SECONDS);
    }

    public Optional<RefreshToken> findByTokenId(String tokenId) {
        String key = PREFIX + tokenId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        return Optional.of(objectMapper.convertValue(value, RefreshToken.class));
    }


    public Optional<RefreshToken> findByRefreshToken(String refreshTokenValue) {
        String tokenId = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_INDEX + refreshTokenValue);
        if (tokenId == null) return Optional.empty();
        return findByTokenId(tokenId);
    }

    public Optional<RefreshToken> findByUserId(Long userId) {
        String tokenId = (String) redisTemplate.opsForValue().get(USER_ID_INDEX + userId);
        if (tokenId == null) return Optional.empty();
        return findByTokenId(tokenId);
    }

    public void deleteByTokenId(String tokenId) {
        findByTokenId(tokenId).ifPresent(token -> {
            // 인덱스도 같이 제거
            redisTemplate.delete(REFRESH_TOKEN_INDEX + token.getRefreshToken());
            redisTemplate.delete(USER_ID_INDEX + token.getUserId());
        });
        redisTemplate.delete(PREFIX + tokenId);
    }

    public void deleteByRefreshToken(String refreshTokenValue) {
        findByRefreshToken(refreshTokenValue).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }

    public void deleteByUserId(Long userId) {
        findByUserId(userId).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }
}
