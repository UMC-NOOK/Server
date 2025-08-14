package umc.nook.users.redis;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.NoRepositoryBean;
import umc.nook.users.domain.RefreshToken;
import umc.nook.users.domain.User;

import java.util.Optional;

@NoRepositoryBean
public interface RefreshTokenRedisRepository extends KeyValueRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
    
    Optional<RefreshToken> findByTokenId(String tokenId);

    void deleteByUser(User user);

    void deleteByTokenId(String tokenId);
}
