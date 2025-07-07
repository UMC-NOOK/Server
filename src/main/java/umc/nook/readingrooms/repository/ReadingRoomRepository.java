package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.ReadingRoom;

public interface ReadingRoomRepository extends JpaRepository<ReadingRoom, Long> {
}
