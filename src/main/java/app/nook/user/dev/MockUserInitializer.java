package app.nook.user.dev;

import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MockUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfNotExists(
                "dev@test.com",
                "DEV_USER"
        );

        createIfNotExists(
                "admin@test.com",
                "DEV_ADMIN"
        );

        log.info("[MOCK USER INIT] 완료");
    }

    private void createIfNotExists(
            String email,
            String nickname
    ) {
        boolean exists = userRepository.existsByEmail(email);
        if (exists) {
            log.info("[MOCK USER EXISTS] email={}", email);
            return;
        }

        User user = User.builder()
                .email(email)
                .nickName(nickname)
                .provider("DEV")
                .providerId("DEV_" + email)
                .build();

        userRepository.save(user);
        log.info("[MOCK USER CREATED] email={}", email);
    }
}
