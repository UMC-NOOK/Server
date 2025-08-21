package umc.nook.users.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class KakaoRefreshTokenRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "kakaoRefreshToken:"; // 본문 저장 키
    private static final String USER_ID_INDEX = "kakaoRefreshToken:userId:";
    private static final String REFRESH_TOKEN_INDEX = "kakaoRefreshToken:refreshToken:";
    private static final long TTL_SECONDS = 60 * 60 * 24 * 60; // 60일

    public void save(KakaoRefreshToken kakaoRefreshToken) {
        String mainKey = PREFIX + kakaoRefreshToken.getTokenId();

        // 본문 저장
        redisTemplate.opsForValue().set(mainKey, kakaoRefreshToken, TTL_SECONDS, TimeUnit.SECONDS);

        // 인덱스 저장
        redisTemplate.opsForValue().set(USER_ID_INDEX + kakaoRefreshToken.getUserId(),
                kakaoRefreshToken.getTokenId(), TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_INDEX + kakaoRefreshToken.getRefreshToken(),
                kakaoRefreshToken.getTokenId(), TTL_SECONDS, TimeUnit.SECONDS);
    }

    public Optional<KakaoRefreshToken> findByTokenId(String tokenId) {
        String mainKey = PREFIX + tokenId;
        return Optional.ofNullable((KakaoRefreshToken) redisTemplate.opsForValue().get(mainKey));
    }

    public Optional<KakaoRefreshToken> findByUserId(Long userId) {
        String tokenId = (String) redisTemplate.opsForValue().get(USER_ID_INDEX + userId);
        if (tokenId == null) return Optional.empty();
        return findByTokenId(tokenId);
    }

    public Optional<KakaoRefreshToken> findByRefreshToken(String refreshToken) {
        String tokenId = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_INDEX + refreshToken);
        if (tokenId == null) return Optional.empty();
        return findByTokenId(tokenId);
    }

    public void deleteByTokenId(String tokenId) {
        findByTokenId(tokenId).ifPresent(token -> {
            redisTemplate.delete(USER_ID_INDEX + token.getUserId());
            redisTemplate.delete(REFRESH_TOKEN_INDEX + token.getRefreshToken());
        });
        redisTemplate.delete(PREFIX + tokenId);
    }

    public void deleteByUserId(Long userId) {
        findByUserId(userId).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }

    public void deleteByRefreshToken(String refreshToken) {
        findByRefreshToken(refreshToken).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }
}
