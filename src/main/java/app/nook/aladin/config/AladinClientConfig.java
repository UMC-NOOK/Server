package app.nook.aladin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AladinClientConfig {
    @Bean
    public RestClient aladinRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
