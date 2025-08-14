package umc.nook.users.redis;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.NoRepositoryBean;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;

@NoRepositoryBean
public interface KakaoRefreshTokenRedisRepository extends KeyValueRepository<KakaoRefreshToken, String> {

    Optional<KakaoRefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    Optional<KakaoRefreshToken> findByRefreshToken(String refreshToken);
}
