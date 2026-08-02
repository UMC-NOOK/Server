package app.nook.user.oauth;

import app.nook.admin.AdminAccessChecker;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    private OAuthService oAuthService;
    private MockRestServiceServer server;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenRedisRepository tokenRedisRepository;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        oAuthService = new OAuthService(
                builder,
                objectMapper,
                userRepository,
                jwtProvider,
                tokenRedisRepository,
                adminAccessChecker
        );

        ReflectionTestUtils.setField(oAuthService, "googleClientId", "google-client-id");
        ReflectionTestUtils.setField(oAuthService, "googleClientSecret", "google-client-secret");
        ReflectionTestUtils.setField(oAuthService, "googleRedirectUri", "https://app.test/redirect");
        ReflectionTestUtils.setField(oAuthService, "googleTokenUri", "https://google.test/token");
        ReflectionTestUtils.setField(oAuthService, "googleUserInfoUri", "https://google.test/userinfo");
    }

    @Test
    void 소셜로그인_기존유저_성공() {
        String tokenJson = """
                {
                  "access_token": "google-access",
                  "expires_in": 3600,
                  "scope": "email profile",
                  "token_type": "Bearer",
                  "id_token": "id-token",
                  "refresh_token": "google-refresh"
                }
                """;

        String userInfoJson = """
                {
                  "sub": "google-sub",
                  "email": "user@test.com",
                  "name": "user",
                  "picture": "https://img.test/user.png"
                }
                """;

        server.expect(once(), requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("redirect_uri=https%3A%2F%2Fapp.test%2Fredirect")))
                .andRespond(withSuccess(tokenJson, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://google.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));

        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        given(userRepository.findByEmail(eq("user@test.com")))
                .willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user)).willReturn("app-access");
        given(jwtProvider.createRefreshToken()).willReturn("app-refresh");

        UserDTO.LoginResponse result = oAuthService.login("google", "auth-code");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getEmail()).isEqualTo("user@test.com");
        assertThat(result.getNickName()).isEqualTo("user");
        assertThat(result.getAccessToken()).isEqualTo("app-access");
        assertThat(result.isOnboardingCompleted()).isFalse();
        ArgumentCaptor<TokenRedis> tokenCaptor = ArgumentCaptor.forClass(TokenRedis.class);
        verify(tokenRedisRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(tokenCaptor.getValue().getAccessToken()).isEqualTo("app-access");
        assertThat(tokenCaptor.getValue().getRefreshToken()).isEqualTo("app-refresh");
        server.verify();
    }

    @Test
    void 소셜로그인_신규유저_생성() {
        String tokenJson = """
                {
                  "access_token": "google-access",
                  "expires_in": 3600,
                  "scope": "email profile",
                  "token_type": "Bearer",
                  "id_token": "id-token",
                  "refresh_token": "google-refresh"
                }
                """;

        String userInfoJson = """
                {
                  "sub": "google-sub-new",
                  "email": "new@test.com",
                  "name": "new-user",
                  "picture": "https://img.test/new.png"
                }
                """;

        server.expect(once(), requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("redirect_uri=https%3A%2F%2Fapp.test%2Fredirect")))
                .andRespond(withSuccess(tokenJson, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://google.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));

        given(userRepository.findByEmail(eq("new@test.com")))
                .willReturn(Optional.empty());

        User savedUser = User.builder()
                .email("new@test.com")
                .nickName("new-user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("google-sub-new")
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 20L);

        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtProvider.createAccessToken(savedUser)).willReturn("app-access-new");
        given(jwtProvider.createRefreshToken()).willReturn("app-refresh-new");

        UserDTO.LoginResponse result = oAuthService.login("google", "auth-code");

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getNickName()).isEqualTo("new-user");
        assertThat(result.getAccessToken()).isEqualTo("app-access-new");
        assertThat(result.isOnboardingCompleted()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("GOOGLE");
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("google-sub-new");
        server.verify();
    }

    @Test
    void 소셜로그인_관리자이메일이면_role이_ADMIN으로_동기화된다() {
        String tokenJson = """
                {
                  "access_token": "google-access",
                  "token_type": "Bearer",
                  "refresh_token": "google-refresh",
                  "expires_in": 3600
                }
                """;

        String userInfoJson = """
                {
                  "sub": "google-sub-admin",
                  "email": "admin@test.com",
                  "name": "admin-user",
                  "picture": "https://img.test/admin.png"
                }
                """;

        server.expect(once(), requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://google.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));

        User user = User.builder()
                .email("admin@test.com")
                .nickName("admin-user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("google-sub-admin")
                .build();
        ReflectionTestUtils.setField(user, "id", 50L);

        given(userRepository.findByEmail(eq("admin@test.com")))
                .willReturn(Optional.of(user));
        given(adminAccessChecker.isAdmin("admin@test.com")).willReturn(true);
        given(jwtProvider.createAccessToken(user)).willReturn("app-access");
        given(jwtProvider.createRefreshToken()).willReturn("app-refresh");

        oAuthService.login("google", "auth-code");

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        server.verify();
    }

    @Test
    void 소셜로그인_탈퇴유예중_계정이면_recovery토큰을_반환하고_로그인토큰은_발급하지_않는다() {
        String tokenJson = """
                {
                  "access_token": "google-access",
                  "expires_in": 3600,
                  "scope": "email profile",
                  "token_type": "Bearer",
                  "id_token": "id-token",
                  "refresh_token": "google-refresh"
                }
                """;

        String userInfoJson = """
                {
                  "sub": "google-sub-gone",
                  "email": "gone@test.com",
                  "name": "gone-user",
                  "picture": "https://img.test/gone.png"
                }
                """;

        server.expect(once(), requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://google.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));

        User user = User.builder()
                .email("gone@test.com")
                .nickName("gone-user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("google-sub-gone")
                .build();
        ReflectionTestUtils.setField(user, "id", 40L);
        user.withdraw(); // soft delete(탈퇴 유예중) 상태

        given(userRepository.findByEmail(eq("gone@test.com")))
                .willReturn(Optional.of(user));
        given(jwtProvider.createRecoveryToken(user)).willReturn("recovery-token");

        UserDTO.LoginResponse result = oAuthService.login("google", "auth-code");

        assertThat(result.getRecoveryRequired()).isTrue();
        assertThat(result.getRecoveryToken()).isEqualTo("recovery-token");
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRefreshToken()).isNull();
        verify(tokenRedisRepository, never()).save(any());
        server.verify();
    }

    @Test
    void 소셜로그인_온보딩완료된_기존유저_onboardingCompleted_true() {
        String tokenJson = """
                {
                  "access_token": "google-access",
                  "expires_in": 3600,
                  "scope": "email profile",
                  "token_type": "Bearer",
                  "id_token": "id-token",
                  "refresh_token": "google-refresh"
                }
                """;

        String userInfoJson = """
                {
                  "sub": "google-sub-done",
                  "email": "done@test.com",
                  "name": "done-user",
                  "picture": "https://img.test/done.png"
                }
                """;

        server.expect(once(), requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://google.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));

        User user = User.builder()
                .email("done@test.com")
                .nickName("done-user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("google-sub-done")
                .build();
        ReflectionTestUtils.setField(user, "id", 30L);
        ReflectionTestUtils.setField(user, "onboardingCompletedAt", LocalDateTime.now());

        given(userRepository.findByEmail(eq("done@test.com")))
                .willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user)).willReturn("app-access-done");
        given(jwtProvider.createRefreshToken()).willReturn("app-refresh-done");

        UserDTO.LoginResponse result = oAuthService.login("google", "auth-code");

        assertThat(result.isOnboardingCompleted()).isTrue();
        server.verify();
    }
}
