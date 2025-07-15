package umc.nook.common.config.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;

import java.util.Map;

//쿼리스트링에서 액세스 토큰 추출할 때 사용하는데 일단 보류
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 쿼리 스트링에서 토큰 추출
            String token = httpRequest.getParameter("token");

            if (token != null && !token.isEmpty()) {
                attributes.put("accessToken", token); // WebSocket 세션 속성에 저장
            }
        }
        return true; // 계속 handshake 진행
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 필요 시 핸드셰이크 후 처리 가능 (지금은 생략)
    }
}
