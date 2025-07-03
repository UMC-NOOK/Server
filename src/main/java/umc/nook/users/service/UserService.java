package umc.nook.users.service;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.users.domain.RoleType;
import umc.nook.users.domain.Status;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.repository.RefreshTokenRepository;
import umc.nook.users.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

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

}
