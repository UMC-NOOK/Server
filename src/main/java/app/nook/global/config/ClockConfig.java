package app.nook.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
