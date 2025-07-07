package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.domain.ReadingRoomUser;
import umc.nook.users.domain.User;

public interface ReadingRoomUserRepository extends JpaRepository<ReadingRoomUser, Long> {
    boolean existsByReadingRoomAndUser(ReadingRoom room, User user);
    int countByReadingRoom(ReadingRoom readingRoom);
}
