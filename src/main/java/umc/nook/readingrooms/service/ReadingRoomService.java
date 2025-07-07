package umc.nook.readingrooms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.repository.ReadingRoomRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingRoomService {

    private final ReadingRoomRepository readingRoomRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public List<ReadingRoomDTO.ReadingRoomResponseDTO> getAllReadingRooms(int page) {

        int pageSize = 16;
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<ReadingRoom> readingRooms = readingRoomRepository.findAll(pageRequest);

        return readingRooms.stream().map(room -> {

            String connectedKey = "ReadingRoom:" + room.getId() + ":users";
            String joinedKey = "ReadingRoom:" + room.getId() + ":joinedUsers";

            Long connectedCount = redisTemplate.opsForSet().size(connectedKey);
            Long joinedCount = redisTemplate.opsForSet().size(joinedKey);

            List<String> hashtagNames = room.getHashtags().stream()
                    .map(hashtag -> hashtag.getHashtag().getName().name()) // enum이면 .name()
                    .toList();

            return ReadingRoomDTO.ReadingRoomResponseDTO.builder()
                    .roomId(room.getId())
                    .name(room.getName())
                    .description(room.getDescription())
                    .hashtags(hashtagNames)
                    .currentUserCount(connectedCount != null ? connectedCount.intValue() : 0)
                    .totalUserCount(joinedCount != null ? joinedCount.intValue() : 0)
                    .build();

        }).collect(Collectors.toList());
    }
}
