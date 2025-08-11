package umc.nook.users.service;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.profile.domain.Profile;
import umc.nook.users.domain.KakaoRefreshToken;
import umc.nook.users.domain.RoleType;
import umc.nook.users.domain.Status;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.oauth.KakaoResponseParams;
import umc.nook.users.oauth.OAuthService;
import umc.nook.users.repository.KakaoRefreshTokenRepository;
import umc.nook.users.repository.RefreshTokenRepository;
import umc.nook.users.repository.UserRepository;

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
    public UserDTO.LoginResponseDTO login(UserDTO.LoginDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        UserDTO.TokenResponseDto tokenResponseDto = new UserDTO.TokenResponseDto(accessToken,refreshToken);

        return new UserDTO.LoginResponseDTO(user,tokenResponseDto);
    }


    // 엑세스 토큰 재발급
    @Transactional
    public UserDTO.TokenResponseDto reissue(HttpServletRequest request) {
        String refreshToken = jwtProvider.extractRefreshToken(request)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        Long userId = jwtProvider.parseRefreshToken(refreshToken);

        log.info("userId : {}" , userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken(user);

        // 기존 리프레시 토큰 DB에서 삭제
        refreshTokenRepository.deleteByRefreshToken(refreshToken);

        return UserDTO.TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // 로그아웃
    @Transactional
    public void logout(String refreshToken) {
        jwtProvider.deleteRefreshToken(refreshToken);
    }

    // 유저 정보 반환
    @Transactional
    public UserDTO.UserResponseDTO getUserInfo(User user) {
        return new UserDTO.UserResponseDTO(user);
    }

    // 카카오 로그인
    @Transactional
    public UserDTO.KakaoLoginResponseDTO kakaoLogin(String code) {
        return oAuthService.loginWithKakaoCode(code);
    }



    @Transactional
    public UserDTO.TokenResponseDto kakaoReissue(HttpServletRequest request) {
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

        return UserDTO.TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
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
