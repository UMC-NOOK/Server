package umc.nook.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.users.domain.RefreshToken;
import umc.nook.users.domain.User;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByRefreshToken(String refreshToken);

    void deleteByUser(User user);
}
