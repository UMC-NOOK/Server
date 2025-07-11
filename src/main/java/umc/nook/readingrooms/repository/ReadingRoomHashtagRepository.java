package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.domain.ReadingRoomHashtag;

public interface ReadingRoomHashtagRepository extends JpaRepository<ReadingRoomHashtag, Long> {
    void deleteByReadingRoom(ReadingRoom readingRoom);
}
