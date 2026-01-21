package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.user.domain.User;
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
    @Transactional(readOnly = true)
    public UserDTO.LoginResponse devLogin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[DEV LOGIN FAIL] user not found. email={}", email);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build()
        );

        log.info("[DEV LOGIN SUCCESS] userId={}, email={}", user.getId(), email);

        return new UserDTO.LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickName()
        );
    }

    @Transactional(readOnly = true)
    public UserDTO.LoginResponse getThisUser(User user) {
            return new UserDTO.LoginResponse(user.getId(),user.getEmail(), user.getNickName());
    }
}
