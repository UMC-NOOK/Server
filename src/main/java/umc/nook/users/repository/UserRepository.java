package umc.nook.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.users.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUserId(Long userId);
}
