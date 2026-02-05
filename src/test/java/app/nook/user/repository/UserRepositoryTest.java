package app.nook.user.repository;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_존재() {
        User user = User.builder()
                .email("repo@test.com")
                .nickName("repo")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("repo@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("repo@test.com");
    }

    @Test
    void existsByEmail_확인() {
        User user = User.builder()
                .email("exists@test.com")
                .nickName("exists")
                .role(UserRole.USER)
                .provider("KAKAO")
                .providerId("provider-2")
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@test.com")).isFalse();
    }
}
