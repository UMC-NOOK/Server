package app.nook.user.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * Authorization 헤더에서 Bearer access token 추출 공용 유틸
 */
public final class BearerTokenResolver {

    private BearerTokenResolver() {
    }

    /** Authorization 헤더의 Bearer access token 반환 (없거나 형식 불일치 시 null) */
    public static String resolve(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(JwtProvider.BEARER_PREFIX)) {
            return authorization.substring(JwtProvider.BEARER_PREFIX.length());
        }
        return null;
    }
}
