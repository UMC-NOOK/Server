package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenBlacklistService;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String RECOVERY_USED_PREFIX = "recovery:used:";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenRedisRepository tokenRedisRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * DEV 전용 로그인
     * - 회원가입 x
     * - 기존 유저만 허용
     * - email 기준
     */
    @Transactional
    public UserDTO.LoginResponse devLogin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[DEV LOGIN FAIL] user not found. email={}", email);
                    return new CustomException(AuthErrorCode.USER_NOT_FOUND);
                });

        validateDevUserRole(user);

        log.info("[DEV LOGIN SUCCESS] userId={}, email={}", user.getId(), email);

        return issueTokens(user);
    }

    /**
     * DEV 전용 회원가입
     * - email 중복 불가
     * - USER 권한으로 생성
     */
    @Transactional
    public UserDTO.LoginResponse devSignUp(UserDTO.DevSignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(AuthErrorCode.EMAIL_DUPLICATE);
        }

        User user = User.builder()
                .email(request.getEmail())
                .nickName(request.getNickName())
                .provider("DEV")
                .providerId("DEV_" + request.getEmail())
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[DEV SIGNUP SUCCESS] userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return UserDTO.LoginResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .nickName(savedUser.getNickName())
                .onboardingCompleted(savedUser.isOnboardingCompleted())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDTO.UserInfo getThisUser(User user) {
            return new UserDTO.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getNickName()
            );
    }

    @Transactional
    public UserDTO.TokenReissueResponse reissueAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            if (jwtProvider.isExpiredToken(refreshToken)) {
                throw new CustomException(AuthErrorCode.TOKEN_EXPIRED);
            }
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        TokenRedis tokenRedis = tokenRedisRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("[TOKEN REISSUE REUSE DETECTED] refresh token was valid but not found in Redis. Possible rotated token reuse.");
                    throw new CustomException(AuthErrorCode.INVALID_TOKEN);
                });

        User user = userRepository.findById(tokenRedis.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .refreshToken(newRefreshToken)
                        .accessToken(newAccessToken)
                        .build()
        );

        return new UserDTO.TokenReissueResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     * 1. refresh token 제거 (재발급 차단)
     * 2. access token 을 남은 만료 시간만큼 블랙리스트 처리 (즉시 무효화)
     */
    @Transactional
    public void logout(User user, String accessToken) {
        tokenRedisRepository.deleteById(user.getId());
        tokenBlacklistService.blacklist(accessToken);
        log.info("[LOGOUT] userId={}", user.getId());
    }

    /**
     * 탈퇴 유예중 계정 복구 후 로그인.
     * 로그인 응답으로 받은 recovery token(1시간, type=RECOVERY)으로 호출한다.
     */
    @Transactional
    public UserDTO.LoginResponse recover(String recoveryToken) {
        if (!jwtProvider.validateToken(recoveryToken) || !jwtProvider.isRecoveryToken(recoveryToken)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        Number userIdClaim = jwtProvider.parseClaims(recoveryToken).get("userId", Number.class);
        if (userIdClaim == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        // recovery token 1회용 검증 (replay 차단)
        String usedKey = RECOVERY_USED_PREFIX + recoveryToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(usedKey))) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userIdClaim.longValue())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        // 남은 만료 시간만큼 사용 처리 (TTL)
        markRecoveryTokenUsed(usedKey, recoveryToken);

        if (user.isDeleted()) {
            user.reactivate();
            log.info("[RECOVER] userId={}", user.getId());
        }

        return issueTokens(user);
    }

    private void markRecoveryTokenUsed(String usedKey, String recoveryToken) {
        long remainingMillis = jwtProvider.parseClaims(recoveryToken).getExpiration().getTime()
                - System.currentTimeMillis();
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(usedKey, "used", Duration.ofMillis(remainingMillis));
        }
    }

    private void validateDevUserRole(User user) {
        // DEV 로그인은 USER 권한만 허용 — 다른 역할은 존재하지 않는 것과 동일하게 처리
        if (user.getRole() != UserRole.USER) {
            throw new CustomException(AuthErrorCode.USER_NOT_FOUND);
        }
    }

    private UserDTO.LoginResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .refreshToken(refreshToken)
                        .accessToken(accessToken)
                        .build()
        );

        return UserDTO.LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }
}
