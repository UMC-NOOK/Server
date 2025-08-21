package umc.nook.users.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
@RequiredArgsConstructor
public class KakaoRefreshTokenRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "kakaoRefreshToken:";                 // 본문 저장 키
    private static final String USER_ID_INDEX = "kakaoRefreshToken:userId:";   // userId -> tokenId
    private static final String REFRESH_TOKEN_INDEX = "kakaoRefreshToken:refreshToken:"; // refreshToken -> tokenId

    private static final long MAX_TTL_SECONDS = 60L * 60 * 24 * 60; // 60일

    /** 저장: 필수값 검증 + 고정 TTL 적용 */
    public void save(KakaoRefreshToken token) {
        log.info("[KAKAO-LOGIN] tokenId={}, userId={}, refreshToken={}",
                token.getTokenId(),
                token.getUserId(),
                token.getRefreshToken()
        );

        validateForSave(token);

        String mainKey = PREFIX + token.getTokenId();

        // 본문 저장
        redisTemplate.opsForValue().set(mainKey, token, MAX_TTL_SECONDS, TimeUnit.SECONDS);

        // 인덱스 저장 (동일 TTL 유지)
        redisTemplate.opsForValue().set(USER_ID_INDEX + token.getUserId(), token.getTokenId(), MAX_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_INDEX + token.getRefreshToken(), token.getTokenId(), MAX_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** tokenId로 조회 */
    public Optional<KakaoRefreshToken> findByTokenId(String tokenId) {
        if (!StringUtils.hasText(tokenId)) return Optional.empty();

        Object value = redisTemplate.opsForValue().get(PREFIX + tokenId);
        if (value == null) return Optional.empty();

        KakaoRefreshToken token = convert(value);
        if (isExpired(token)) { // 여기서는 사실상 항상 false (만료 판단 로직 없음)
            deleteByTokenId(tokenId);
            throw new KakaoRefreshTokenExpiredException();
        }
        return Optional.of(token);
    }

    /** refreshToken으로 조회 */
    public Optional<KakaoRefreshToken> findByRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) return Optional.empty();

        String tokenId = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_INDEX + refreshToken);
        if (tokenId == null) return Optional.empty();

        return findByTokenId(tokenId);
    }

    /** userId로 조회 */
    public Optional<KakaoRefreshToken> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();

        String tokenId = (String) redisTemplate.opsForValue().get(USER_ID_INDEX + userId);
        if (tokenId == null) return Optional.empty();

        return findByTokenId(tokenId);
    }

    /** 없으면/만료면 예외 */
    public KakaoRefreshToken getRequiredByRefreshToken(String refreshToken) {
        return findByRefreshToken(refreshToken)
                .orElseThrow(KakaoRefreshTokenNotFoundException::new);
    }

    /** 삭제 */
    public void deleteByTokenId(String tokenId) {
        if (!StringUtils.hasText(tokenId)) return;

        findByTokenId(tokenId).ifPresent(token -> {
            redisTemplate.delete(USER_ID_INDEX + token.getUserId());
            redisTemplate.delete(REFRESH_TOKEN_INDEX + token.getRefreshToken());
        });
        redisTemplate.delete(PREFIX + tokenId);
    }

    public void deleteByRefreshToken(String refreshToken) {
        findByRefreshToken(refreshToken).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }

    public void deleteByUserId(Long userId) {
        findByUserId(userId).ifPresent(token -> deleteByTokenId(token.getTokenId()));
    }

    /** ===== 내부 유틸 ===== */
    private void validateForSave(KakaoRefreshToken token) {
        if (token == null
                || !StringUtils.hasText(token.getTokenId())
                || !StringUtils.hasText(token.getRefreshToken())
                || token.getUserId() == null) {
            throw new KakaoRefreshTokenInvalidException("카카오 리프레시 토큰 정보가 올바르지 않습니다.");
        }
    }

    private KakaoRefreshToken convert(Object value) {
        if (value instanceof KakaoRefreshToken) return (KakaoRefreshToken) value;
        try {
            return objectMapper.convertValue(value, KakaoRefreshToken.class);
        } catch (IllegalArgumentException e) {
            throw new KakaoRefreshTokenInvalidException("카카오 토큰 역직렬화 실패");
        }
    }

    /** 만료 판단: expiration 필드가 없으므로 무조건 false */
    private boolean isExpired(KakaoRefreshToken token) {
        return token == null;
    }

    /** ===== 커스텀 예외 ===== */
    public static class KakaoRefreshTokenNotFoundException extends RuntimeException {
        public KakaoRefreshTokenNotFoundException() { super("카카오 리프레시 토큰을 찾을 수 없습니다."); }
    }
    public static class KakaoRefreshTokenExpiredException extends RuntimeException {
        public KakaoRefreshTokenExpiredException() { super("카카오 리프레시 토큰이 만료되었습니다."); }
    }
    public static class KakaoRefreshTokenInvalidException extends RuntimeException {
        public KakaoRefreshTokenInvalidException(String msg) { super(msg); }
    }
}
