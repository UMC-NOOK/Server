package umc.nook.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.users.domain.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from User u " +
            "where u.deletedAt is not null " +
            "and   u.deletedAt <= :threshold " +
            "and   u.status = umc.nook.users.domain.Status.INACTIVE")
    int hardDeleteUsersOlderThan(@Param("threshold") LocalDateTime threshold);

    Optional<User> findByKakaoUserId(Long kaKaoUserId);
}
