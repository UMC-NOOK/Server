package app.nook.yes24.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class Yes24ClientConfig {

    @Bean
    public RestClient yes24RestClient(
            RestClient.Builder builder,
            @Value("${yes24.base-url}") String baseUrl,
            @Value("${yes24.api-key}") String apiKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }
}
