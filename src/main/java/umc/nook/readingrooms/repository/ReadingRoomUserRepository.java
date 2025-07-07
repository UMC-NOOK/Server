package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.ReadingRoomUser;

public interface ReadingRoomUserRepository extends JpaRepository<ReadingRoomUser, Long> {
    int countByReadingRoomId(Long roomId);
}
