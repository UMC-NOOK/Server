package umc.nook.aladin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AladinWebClientConfig {

    @Value("${aladin.base-url}")
    private String aladinBaseUrl;


    @Bean
    public WebClient aladinWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(aladinBaseUrl)
                .build();
    }
}
