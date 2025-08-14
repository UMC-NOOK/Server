package umc.nook.users.service;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.profile.domain.Profile;
import umc.nook.users.domain.*;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.oauth.KakaoResponseParams;
import umc.nook.users.oauth.OAuthService;
import umc.nook.users.redis.KakaoRefreshTokenRepository;
import umc.nook.users.redis.RefreshTokenRepository;
import umc.nook.users.repository.UserRepository;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import java.time.Duration;
import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenRepository refreshTokenRepository;

    private final KakaoRefreshTokenRepository kakaoRefreshTokenRepository;
    private final JwtProvider jwtProvider;

    private final OAuthService oAuthService;

    // 회원가입
    @Transactional
    public UserDTO.UserResponseDTO signup(UserDTO.SignUpDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(RoleType.USER)
                .status(Status.ACTIVE)
                .build();

        Profile profile = Profile.builder().build();
        user.setProfile(profile);

        userRepository.save(user);
        return new UserDTO.UserResponseDTO(user);
    }

    // 로그인
    @Transactional
    public UserDTO.LoginResponseDTO login(UserDTO.LoginDto request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        // 토큰 매핑 키 생성
        String tokenId = UUID.randomUUID().toString();

        // Redis에 저장 (tokenId -> refreshToken)
        RefreshToken tokenEntity = RefreshToken.builder()
                .tokenId(tokenId)
                .refreshToken(refreshToken)
                .build();
        refreshTokenRepository.save(tokenEntity);

        // 쿠키에는 tokenId만 저장
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshTokenId", tokenId)
                .httpOnly(true)
                .secure(false) // 운영에서는 true
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(3))
                .build();
        response.setHeader("Set-Cookie", refreshTokenCookie.toString());

        UserDTO.TokenResponseDto tokenResponseDto = new UserDTO.TokenResponseDto(accessToken);
        return new UserDTO.LoginResponseDTO(user, tokenResponseDto);
    }



    // 엑세스 토큰 재발급
    @Transactional
    public UserDTO.TokenResponseDto reissue(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 tokenId 추출
        String tokenId = extractCookie(request, "refreshTokenId");
        if (tokenId == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Redis에서 Refresh Token 조회
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // Refresh Token 유효성 검증 & 사용자 조회
        Long userId = jwtProvider.parseRefreshToken(tokenEntity.getRefreshToken());
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Access Token 발급
        String newAccessToken = jwtProvider.createAccessToken(user);

        // Refresh Token 남은 기간 확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = tokenEntity.getExpiration(); // 저장 시 넣어둔 만료 시간
        long remainingDays = ChronoUnit.DAYS.between(now, expiration);

        // Refresh Token 교체 여부 판단, 남은 기간이 1일 이하일 경우 Rolling
        String finalTokenId = tokenId;
        if (remainingDays <= 1) {
            String newRefreshToken = jwtProvider.createRefreshToken(user);
            String newTokenId = UUID.randomUUID().toString();

            // Redis 갱신
            refreshTokenRepository.deleteByTokenId(tokenId);
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .tokenId(newTokenId)
                            .refreshToken(newRefreshToken)
                            .expiration(LocalDateTime.now().plusDays(3))
                            .build()
            );

            // 쿠키 갱신
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshTokenId", newTokenId)
                    .httpOnly(true)
                    .secure(false) // 운영 시 true
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(3))
                    .build();
            response.setHeader("Set-Cookie", refreshTokenCookie.toString());

            finalTokenId = newTokenId;
        }

        // Access Token 반환 (Refresh Token은 쿠키로만 관리)
        return UserDTO.TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .build();
    }

    // 로그아웃
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String tokenId = extractCookie(request, "refreshTokenId");
        if (tokenId != null) {
            refreshTokenRepository.deleteByTokenId(tokenId);
        }

        // 쿠키 제거
        ResponseCookie deleteCookie = ResponseCookie.from("refreshTokenId", "")
                .httpOnly(true)
                .secure(false) // 운영 시 true
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.setHeader("Set-Cookie", deleteCookie.toString());
    }

    // 유저 정보 반환
    @Transactional
    public UserDTO.UserResponseDTO getUserInfo(User user) {
        return new UserDTO.UserResponseDTO(user);
    }

    // 카카오 로그인
    @Transactional
    public UserDTO.KakaoLoginResponseDTO kakaoLogin(String code, HttpServletResponse response) {

        UserDTO.KakaoLoginResponseDTO kakaoResponse = oAuthService.loginWithKakaoCode(code);
        // 1. JWT 리프레시 토큰 쿠키
        ResponseCookie jwtRefreshTokenCookie = ResponseCookie.from("refreshToken", kakaoResponse.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(3))
                .build();

        // 2. 카카오 리프레시 토큰 쿠키
        ResponseCookie kakaoRefreshTokenCookie = ResponseCookie.from("kakaoRefreshToken", kakaoResponse.getKakaoRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(60))
                .build();

        response.setHeader("Set-Cookie", jwtRefreshTokenCookie.toString());
        response.addHeader("Set-Cookie", kakaoRefreshTokenCookie.toString());
        return kakaoResponse;
    }

    // 카카오 토큰 재발급
    @Transactional
    public UserDTO.TokenResponseDto kakaoReissue(HttpServletRequest request, HttpServletResponse response) {
        String kakaoRefreshToken = extractCookie(request, "kakaoRefreshToken");
        if (kakaoRefreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_KAKAO_REFRESH_TOKEN);
        }

        KakaoResponseParams newToken = oAuthService.reissueKakaoToken(kakaoRefreshToken);

        KakaoRefreshToken savedToken = kakaoRefreshTokenRepository.findByRefreshToken(kakaoRefreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_KAKAO_REFRESH_TOKEN));

        // 기존 refresh_token 유지 조건
        String updatedRefreshToken = newToken.getRefreshToken() != null
                ? newToken.getRefreshToken()
                : savedToken.getRefreshToken();

        savedToken.updateToken(
                updatedRefreshToken,
                newToken.getAccessToken(),
                (long) newToken.getRefreshTokenExpiresIn()
        );

        Long userId = savedToken.getUserId();

        // JWT 발급
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken(user);

        // 쿠키 갱신
        ResponseCookie refreshTokenCookie = ResponseCookie.from("kakaoRefreshToken", savedToken.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        response.setHeader("Set-Cookie", refreshTokenCookie.toString());

        return UserDTO.TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .build();
    }


    // 카카오 로그아웃
    @Transactional
    public void kakaoLogout(User user) {
        KakaoRefreshToken token = kakaoRefreshTokenRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        // 카카오 서버에 로그아웃 요청
        oAuthService.buildKakaoLogoutRedirectUrl();
        // 저장된 토큰 삭제
        kakaoRefreshTokenRepository.deleteByUserId(user.getUserId());
    }

    // 쿠키 추출
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // 회원 탈퇴
    @Transactional
    public String withdrawUser(User user) {
        // 이미 탈퇴 처리된 경우
        if (user.getDeletedAt() != null || user.getStatus() == Status.INACTIVE) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }
        if (Boolean.TRUE.equals(user.getIsKakao()) && user.getKakaoUserId() != null) {
            try {
                oAuthService.unlinkWithAdminKey(user.getUserId());
            } catch (Exception e) {
                log.error("카카오 unlink 실패 (탈퇴는 계속 진행). userId={}, err={}", user.getUserId(), e.getMessage(), e);
            }
        }
        user.setDeletedAt(LocalDateTime.now());
        log.info("사용자 탈퇴 완료");
        user.setStatus(Status.INACTIVE);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        return "회원 탈퇴가 완료되었습니다.";
    }

}
