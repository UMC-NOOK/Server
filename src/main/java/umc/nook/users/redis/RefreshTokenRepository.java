package umc.nook.users.redis;

import org.springframework.data.repository.CrudRepository;
import umc.nook.users.domain.RefreshToken;
import umc.nook.users.domain.User;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
    
    Optional<RefreshToken> findByTokenId(String tokenId);

    void deleteByUser(User user);

    void deleteByTokenId(String tokenId);
}
