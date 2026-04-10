package app.nook.user.filter;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import app.nook.user.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenRedisRepository tokenRedisRepository;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String accessToken = resolveAccessToken(request);
        // 엑세스 토큰 검증
        if (accessToken != null) {
            if (jwtProvider.validateToken(accessToken)) {
                // 만료되지 않으면 SecurityContext 에 저장
                setAuthentication(accessToken);
            } else if (jwtProvider.isExpiredToken(accessToken)) {
                // 만료 시 재발급
                reissueAccessToken(accessToken, response);
            } else {
                log.warn("[TOKEN] 유효하지 않은 access token. 재발급 시도 생략");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(JwtProvider.BEARER_PREFIX)) {
            return authorization.substring(JwtProvider.BEARER_PREFIX.length());
        }
        return null;
    }

    private void setAuthentication(String accessToken) {
        String email = jwtProvider.extractEmail(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // 토큰 재발급
    private void reissueAccessToken(String expiredAccessToken, HttpServletResponse response) {
        try {
            Number userIdClaim = jwtProvider.parseClaims(expiredAccessToken).get("userId", Number.class);
            if (userIdClaim == null) {
                return;
            }
            Long userId = userIdClaim.longValue();

            tokenRedisRepository.findById(userId).ifPresentOrElse(tokenRedis -> {
                String refreshToken = tokenRedis.getRefreshToken();
                if (!jwtProvider.validateToken(refreshToken)) {
                    log.warn("[TOKEN] 리프레시 토큰 무효. accessToken 재발급 실패");
                    return;
                }

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

                String newAccessToken = jwtProvider.createAccessToken(user);

                // 새로운 액세스 토큰 발급 시 리프레시 토큰도 함께 재발급
                TokenRedis newTokenRedis = TokenRedis.builder()
                        .id(user.getId())
                        .refreshToken(refreshToken)
                        .build();
                tokenRedisRepository.save(newTokenRedis);

                response.setHeader(HttpHeaders.AUTHORIZATION, JwtProvider.BEARER_PREFIX + newAccessToken);
                log.info("[TOKEN] 토큰 만료, 새로운 토큰 발급 ={}", newAccessToken);
                setAuthentication(newAccessToken);
            }, () -> log.warn("[TOKEN] 저장된 리프레시 토큰 없음. accessToken 재발급 실패"));
        } catch (Exception e) {
            log.warn("[TOKEN] access token 재발급 실패");
        }
    }
}
