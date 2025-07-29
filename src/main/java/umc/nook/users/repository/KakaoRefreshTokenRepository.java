package umc.nook.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;

public interface KakaoRefreshTokenRepository extends JpaRepository<KakaoRefreshToken,Long> {
     void deleteByRefreshToken(String refreshToken);

    Optional<KakaoRefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    String findRefreshTokenByUserId(Long userId);
}
