package app.nook.global.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void 클라이언트_IP는_X_Real_IP를_우선하고_없으면_remoteAddr를_쓴다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("10.0.0.1");

        request.addHeader("X-Real-IP", "203.0.113.7");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }
}
