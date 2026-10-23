package app.nook.aladin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AladinClientConfig {
    // TODO(#382): YES24 전환 완료 시 알라딘 설정과 함께 삭제. 그 전까지 RestClient 기본 주입 대상
    @Primary
    @Bean
    public RestClient aladinRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .requestFactory(factory).
                build();
    }
}
