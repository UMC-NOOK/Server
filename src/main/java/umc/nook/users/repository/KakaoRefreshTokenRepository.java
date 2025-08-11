package umc.nook.users.repository;

import com.querydsl.core.group.GroupBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.users.domain.KakaoRefreshToken;

import java.util.Optional;

public interface KakaoRefreshTokenRepository extends JpaRepository<KakaoRefreshToken,Long> {
     void deleteByRefreshToken(String refreshToken);

    Optional<KakaoRefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    KakaoRefreshToken findRefreshTokenByUserId(Long userId);

    Optional<KakaoRefreshToken> findByRefreshToken(String refreshToken);
}
