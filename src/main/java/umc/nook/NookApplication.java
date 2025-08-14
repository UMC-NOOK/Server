package umc.nook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(
		basePackages = "umc.nook", // JPA 전용 Repository 패키지
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.REGEX,
				pattern = "umc\\.nook\\.users\\.redis\\..*" // Redis 패키지 제외
		)
)
public class NookApplication {

	public static void main(String[] args) {
		SpringApplication.run(NookApplication.class, args);
	}

}
