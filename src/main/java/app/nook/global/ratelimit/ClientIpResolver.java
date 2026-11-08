package app.nook.global.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 공개 경로 요청 제한용 클라이언트 IP 추출 유틸
 * - prod는 nginx가 Cloudflare 대역에서 온 요청만 CF-Connecting-IP로 복원해 X-Real-IP에 담는다.
 * - 앱은 외부 포트를 열지 않아 nginx를 거치지 않고는 접근할 수 없으므로 X-Real-IP를 신뢰한다.
 */
public final class ClientIpResolver {

    private static final String X_REAL_IP = "X-Real-IP";

    private ClientIpResolver() {
    }

    /** X-Real-IP 헤더, 없으면 remoteAddr 반환 */
    public static String resolve(HttpServletRequest request) {
        String realIp = request.getHeader(X_REAL_IP);
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
