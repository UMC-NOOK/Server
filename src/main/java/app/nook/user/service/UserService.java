package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenRedisRepository tokenRedisRepository;

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
                .build();
    }

    @Transactional(readOnly = true)
    public UserDTO.LoginResponse getThisUser(User user) {
            return UserDTO.LoginResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickName(user.getNickName())
                    .build();
    }

    private void validateDevUserRole(User user) {
        if (user.getRole() != UserRole.USER) {
            throw new CustomException(AuthErrorCode.PERMISSION_DENIED);
        }
    }

    private UserDTO.LoginResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .refreshToken(refreshToken)
                        .build()
        );

        return UserDTO.LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .accessToken(accessToken)
                .build();
    }
}
