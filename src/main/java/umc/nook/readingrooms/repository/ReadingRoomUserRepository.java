package umc.nook.readingrooms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.domain.ReadingRoomUser;
import umc.nook.readingrooms.domain.Role;
import umc.nook.users.domain.User;

import java.util.List;
import java.util.Optional;

public interface ReadingRoomUserRepository extends JpaRepository<ReadingRoomUser, Long> {
    boolean existsByReadingRoomAndUser(ReadingRoom room, User user);
    List<ReadingRoomUser> findByUser(User user);
    int countByReadingRoom(ReadingRoom readingRoom);
    boolean existsByReadingRoomAndUserAndRole(ReadingRoom readingRoom, User user, Role role);
    Optional<ReadingRoomUser> findByReadingRoomAndUser(ReadingRoom readingRoom, User user);
    List<ReadingRoomUser> findAllByReadingRoom(ReadingRoom readingRoom);
    Optional<ReadingRoomUser> findTopByUserOrderByLastAccessedAtDesc(User user);
    int countByUser(User user);
    @Query("SELECT r FROM ReadingRoom r WHERE r.id NOT IN " +
            "(SELECT rru.readingRoom.id FROM ReadingRoomUser rru WHERE rru.user = :user)")
    Page<ReadingRoom> findAllExcludingUserJoinedRooms(@Param("user") User user, Pageable pageable);
}
