package umc.nook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NookApplication {

	public static void main(String[] args) {
		SpringApplication.run(NookApplication.class, args);
	}

}
