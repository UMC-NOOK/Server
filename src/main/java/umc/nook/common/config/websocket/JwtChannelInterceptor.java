package umc.nook.common.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import umc.nook.users.service.JwtProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            String sessionId = accessor.getSessionId();
            StompCommand command = accessor.getCommand();

            log.info(">>> [JwtChannelInterceptor] STOMP Command: {}, sessionId = {}", command, sessionId);

            // CONNECT, SUBSCRIBE, SEND 시 모두 검사
            if (StompCommand.CONNECT.equals(command) ||
                    StompCommand.SUBSCRIBE.equals(command) ||
                    StompCommand.SEND.equals(command)) {

                // 우선 기존에 세션에 Authentication이 있으면 그대로 사용
                Authentication existingAuth = (Authentication) accessor.getUser();
                if (existingAuth != null) {
                    log.debug("Existing Authentication found in accessor for session {}: {}", sessionId, existingAuth.getName());
                    accessor.setUser(existingAuth); // 명시적으로 다시 세팅
                    return message;
                }

                // 없다면 헤더에서 토큰을 읽어서 다시 인증
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    log.info("Authorization Header Token: {}", token);

                    if (jwtProvider.validateToken(token)) {
                        String email = jwtProvider.extractEmail(token);
                        Authentication authentication = jwtProvider.getAuthentication(email);
                        accessor.setUser(authentication);

                        log.info("Authentication set for session {} ({}): {}", sessionId, command, accessor.getUser() != null);
                    } else {
                        log.warn("Invalid JWT token for STOMP {}", command);
                    }
                } else {
                    // 헤더가 없고, 기존 Principal도 없으면 로그 남기기
                    log.warn("No Authorization header or existing Principal for STOMP {}", command);
                }
            }

            return message;
        } catch (Exception e) {
            log.error("Error in JwtChannelInterceptor.preSend: {}", e.getMessage(), e);
            return message;
        }
    }
}
