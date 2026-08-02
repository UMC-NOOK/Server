package app.nook.user.repository;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    /**
     * 지정 상태이면서 deletedAt 이 기준 시각 이전인 유저 조회.
     * soft delete 후 유예기간이 지난 계정을 스케줄러가 완전 삭제하기 위해 사용한다.
     */
    List<User> findByStatusAndDeletedAtBefore(UserStatus status, LocalDateTime threshold);

    /** 고아 이미지 대조용 — 사용 중인 모든 프로필 이미지 key */
    @Query("SELECT u.profileImageKey FROM User u WHERE u.profileImageKey IS NOT NULL")
    List<String> findAllProfileImageKeys();
}
