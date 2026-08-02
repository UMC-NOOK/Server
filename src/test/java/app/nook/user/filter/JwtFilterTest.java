package app.nook.user.filter;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenBlacklistService;
import app.nook.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class DoFilterInternal {

        @Test
        void 유효한_액세스토큰이면_인증정보를_저장한다() throws Exception {
            String accessToken = "valid-access-token";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + accessToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            Claims claims = Jwts.claims();
            claims.put("userId", 1L);

            User user = User.builder()
                    .email("user@test.com")
                    .nickName("tester")
                    .role(UserRole.USER)
                    .provider("DEV")
                    .providerId("dev-1")
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(jwtProvider.validateToken(accessToken)).willReturn(true);
            given(jwtProvider.parseClaims(accessToken)).willReturn(claims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            jwtFilter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void 유효하지않고_만료되지도_않은_토큰이면_JwtException을_던진다() throws Exception {
            String invalidAccessToken = "invalid-access-token";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + invalidAccessToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(jwtProvider.validateToken(invalidAccessToken)).willReturn(false);
            given(jwtProvider.isExpiredToken(invalidAccessToken)).willReturn(false);

            assertThatThrownBy(() -> jwtFilter.doFilter(request, response, filterChain))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        void 만료된_액세스토큰이면_인증정보를_저장하지_않고_재발급도_하지않는다() throws Exception {
            String expiredAccessToken = "expired-access-token";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + expiredAccessToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(jwtProvider.validateToken(expiredAccessToken)).willReturn(false);
            given(jwtProvider.isExpiredToken(expiredAccessToken)).willReturn(true);

            jwtFilter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(response.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
            verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void recovery토큰이면_JwtException을_던지고_인증하지_않는다() throws Exception {
            String recoveryToken = "recovery-token";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + recoveryToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(jwtProvider.validateToken(recoveryToken)).willReturn(true);
            given(jwtProvider.isRecoveryToken(recoveryToken)).willReturn(true);

            assertThatThrownBy(() -> jwtFilter.doFilter(request, response, filterChain))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        void 블랙리스트에_등록된_토큰이면_JwtException을_던진다() throws Exception {
            String blacklistedToken = "blacklisted-token";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + blacklistedToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(jwtProvider.validateToken(blacklistedToken)).willReturn(true);
            given(tokenBlacklistService.isBlacklisted(blacklistedToken)).willReturn(true);

            assertThatThrownBy(() -> jwtFilter.doFilter(request, response, filterChain))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        void Authorization_헤더가_없으면_그대로_필터체인을_진행한다() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            jwtFilter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}
