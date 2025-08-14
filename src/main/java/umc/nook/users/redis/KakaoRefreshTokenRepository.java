package umc.nook.users.redis;

import org.springframework.data.repository.CrudRepository;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;

public interface KakaoRefreshTokenRepository extends CrudRepository<KakaoRefreshToken, String> {

    Optional<KakaoRefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    Optional<KakaoRefreshToken> findByRefreshToken(String refreshToken);
}
