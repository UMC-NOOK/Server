package umc.nook.readingrooms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.domain.ReadingRoomUser;
import umc.nook.readingrooms.domain.Role;
import umc.nook.users.domain.User;

import java.util.List;
import java.util.Optional;

public interface ReadingRoomUserRepository extends JpaRepository<ReadingRoomUser, Long> {
    boolean existsByReadingRoomAndUser(ReadingRoom room, User user);
    Page<ReadingRoomUser> findByUser(User user, Pageable pageable);
    int countByReadingRoom(ReadingRoom readingRoom);
    boolean existsByReadingRoomAndUserAndRole(ReadingRoom readingRoom, User user, Role role);
    Optional<ReadingRoomUser> findByReadingRoomAndUser(ReadingRoom readingRoom, User user);
    List<ReadingRoomUser> findAllByReadingRoom(ReadingRoom readingRoom);
}
