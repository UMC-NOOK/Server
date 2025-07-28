package umc.nook.users.service;


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
import umc.nook.users.oauth.KakaoReissueParams;
import umc.nook.users.oauth.OAuthService;
import umc.nook.users.repository.KakaoRefreshTokenRepository;
import umc.nook.users.repository.RefreshTokenRepository;
import umc.nook.users.repository.UserRepository;

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
    public UserDTO.LoginResponseDTO kakaoLogin(String code) {
        return oAuthService.loginWithKakaoCode(code);
    }


    // 카카오 토큰 재발급
    @Transactional
    public UserDTO.TokenResponseDto kakaoReissue(User user) {
        // 1. 기존 카카오 토큰 조회
        KakaoRefreshToken oldToken = kakaoRefreshTokenRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_KAKAO_REFRESH_TOKEN));

        // 2. 카카오 서버에서 새 토큰 발급 받기
        KakaoReissueParams newToken = oAuthService.reissueKakaoToken(oldToken.getRefreshToken());

        // 3. 기존 토큰 삭제
        kakaoRefreshTokenRepository.deleteByUserId(user.getUserId());

        // 4. 새 토큰 저장
        KakaoRefreshToken token = KakaoRefreshToken.builder()
                .userId(user.getUserId())
                .refreshToken(newToken.getRefresh_token())
                .accessToken(newToken.getAccess_token())
                .refreshTokenExpiresIn((long) newToken.getRefresh_token_expires_in())
                .build();
        kakaoRefreshTokenRepository.save(token);

        // 5. JWT 토큰 재발급
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
        oAuthService.kakaoLogout(token.getAccessToken());
        // 저장된 토큰 삭제
        kakaoRefreshTokenRepository.deleteByUserId(user.getUserId());
    }

    // 카카오 토큰 조회
    @Transactional
    public String viewKakaoRefreshTokenByUser(Long userId) {
        KakaoRefreshToken refreshToken = kakaoRefreshTokenRepository.findRefreshTokenByUserId(userId);
        if (refreshToken==null)
            throw new CustomException(ErrorCode.INVALID_KAKAO_REFRESH_TOKEN);
        return refreshToken.getRefreshToken();
    }
}
