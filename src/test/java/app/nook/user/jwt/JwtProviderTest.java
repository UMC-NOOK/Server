package app.nook.user.jwt;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET_KEY = Base64.getEncoder()
            .encodeToString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());

    private final JwtProvider jwtProvider = createJwtProvider();

    @Nested
    class ValidateToken {

        @Test
        void access토큰은_검증에_성공한다() {
            User user = createUser(1L);
            String accessToken = jwtProvider.createAccessToken(user);

            boolean valid = jwtProvider.validateToken(accessToken);

            assertThat(valid).isTrue();
        }

        @Test
        void refresh토큰은_검증에_성공한다() {
            String refreshToken = jwtProvider.createRefreshToken();

            boolean valid = jwtProvider.validateToken(refreshToken);

            assertThat(valid).isTrue();
        }

        @Test
        void 잘못된_토큰은_검증에_실패한다() {
            boolean valid = jwtProvider.validateToken("invalid-token");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    class ParseClaims {

        @Test
        void access토큰에서_사용자식별클레임을_파싱한다() {
            User user = createUser(7L);
            String accessToken = jwtProvider.createAccessToken(user);

            Claims claims = jwtProvider.parseClaims(accessToken);

            assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(7L);
            assertThat(claims.get("role", String.class)).isEqualTo(UserRole.USER.name());
        }

        @Test
        void 만료된_토큰도_클레임은_반환한다() {
            String expiredAccessToken = io.jsonwebtoken.Jwts.builder()
                    .claim("userId", 1L)
                    .claim("role", UserRole.USER.name())
                    .setIssuedAt(new Date(System.currentTimeMillis() - 2_000L))
                    .setExpiration(new Date(System.currentTimeMillis() - 1_000L))
                    .signWith((java.security.Key) ReflectionTestUtils.getField(jwtProvider, "key"),
                            io.jsonwebtoken.SignatureAlgorithm.HS256)
                    .compact();

            Claims claims = jwtProvider.parseClaims(expiredAccessToken);

            assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(1L);
            assertThat(jwtProvider.isExpiredToken(expiredAccessToken)).isTrue();
        }
    }

    private JwtProvider createJwtProvider() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secretKey", SECRET_KEY);
        jwtProvider.init();
        return jwtProvider;
    }

    private User createUser(Long id) {
        User user = User.builder()
                .email("user@test.com")
                .nickName("tester")
                .role(UserRole.USER)
                .provider("DEV")
                .providerId("dev-1")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
