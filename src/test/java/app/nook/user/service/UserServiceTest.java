package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.global.response.CommonErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenRedisRepository tokenRedisRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void devLogin_성공() {
        User user = User.builder()
                .email("dev@test.com")
                .nickName("DEV_USER")
                .role(UserRole.USER)
                .provider("DEV")
                .providerId("dev-1")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByEmail(eq("dev@test.com")))
                .willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user)).willReturn("access-token");
        given(jwtProvider.createRefreshToken()).willReturn("refresh-token");

        UserDTO.LoginResponse response = userService.devLogin("dev@test.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("dev@test.com");
        assertThat(response.getNickName()).isEqualTo("DEV_USER");
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        ArgumentCaptor<TokenRedis> tokenCaptor = ArgumentCaptor.forClass(TokenRedis.class);
        verify(tokenRedisRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(tokenCaptor.getValue().getAccessToken()).isNull();
        assertThat(tokenCaptor.getValue().getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void devLogin_유저없음_예외() {
        given(userRepository.findByEmail(eq("missing@test.com")))
                .willReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.devLogin("missing@test.com")
        );

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.USER_NOT_FOUND);
    }

    @Test
    void devLogin_ADMIN_권한_예외() {
        User admin = User.builder()
                .email("admin@test.com")
                .nickName("DEV_ADMIN")
                .role(UserRole.ADMIN)
                .provider("DEV")
                .providerId("dev-admin")
                .build();

        given(userRepository.findByEmail(eq("admin@test.com")))
                .willReturn(Optional.of(admin));

        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.devLogin("admin@test.com")
        );

        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.PERMISSION_DENIED);
    }

    @Test
    void devSignUp_성공() {
        UserDTO.DevSignUpRequest request = new UserDTO.DevSignUpRequest("new@test.com", "NEW_USER");

        given(userRepository.existsByEmail(eq("new@test.com"))).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        UserDTO.LoginResponse response = userService.devSignUp(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getEmail()).isEqualTo("new@test.com");
        assertThat(response.getNickName()).isEqualTo("NEW_USER");
        assertThat(response.getAccessToken()).isNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("DEV");
    }

    @Test
    void devSignUp_이메일중복_예외() {
        UserDTO.DevSignUpRequest request = new UserDTO.DevSignUpRequest("dup@test.com", "DUP_USER");
        given(userRepository.existsByEmail(eq("dup@test.com"))).willReturn(true);

        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.devSignUp(request)
        );

        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_DUPLICATE);
    }
}
