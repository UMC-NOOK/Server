package app.nook.user.filter;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.jwt.BearerTokenResolver;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenBlacklistService;
import app.nook.user.repository.UserRepository;
import app.nook.user.service.CustomUserDetails;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String accessToken = BearerTokenResolver.resolve(request);
        // 엑세스 토큰 검증
        if (accessToken != null) {
            if (jwtProvider.validateToken(accessToken)) {
                // recovery 전용 토큰은 일반 API 인증에 사용할 수 없음
                if (jwtProvider.isRecoveryToken(accessToken)) {
                    log.warn("[TOKEN] recovery token 은 API 인증에 사용할 수 없음");
                    throw new JwtException("recovery token 은 API 인증에 사용할 수 없습니다");
                }
                // 로그아웃/탈퇴로 블랙리스트에 등록된 토큰은 거부
                if (tokenBlacklistService.isBlacklisted(accessToken)) {
                    log.warn("[TOKEN] 블랙리스트 처리된 access token (로그아웃/탈퇴)");
                    throw new JwtException("로그아웃된 access token");
                }
                // 만료되지 않으면 SecurityContext 에 저장
                setAuthentication(accessToken);
            } else if (jwtProvider.isExpiredToken(accessToken)) {
                log.warn("[TOKEN] 만료된 access token. /auth/reissue API 사용 필요");
            } else {
                log.warn("[TOKEN] 유효하지 않은 access token");
                throw new JwtException("유효하지 않은 access token");
            }
        }

        filterChain.doFilter(request, response);
    }

    // 엑세스토큰으로 인증 객체 생성, authentication 객체를 SecurityContext에 저장
    private void setAuthentication(String accessToken) {
        Number userIdClaim = jwtProvider.parseClaims(accessToken).get("userId", Number.class);
        if (userIdClaim == null) {
            throw new CustomException(AuthErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userIdClaim.longValue())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
