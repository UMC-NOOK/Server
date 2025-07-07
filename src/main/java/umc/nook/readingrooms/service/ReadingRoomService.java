package umc.nook.readingrooms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.domain.ReadingRoomUser;
import umc.nook.readingrooms.domain.Role;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.repository.ReadingRoomRepository;
import umc.nook.readingrooms.repository.ReadingRoomUserRepository;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetailService;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingRoomService {

    private final ReadingRoomRepository readingRoomRepository;
    private final ReadingRoomUserRepository readingRoomUserRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 전체 리딩룸 조회
    @Transactional(readOnly = true)
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
                    .themeImageUrl(room.getTheme().getImageUrl())
                    .build();

        }).collect(Collectors.toList());
    }

    // 리딩룸 가입, DB + Redis에 저장
    public Long joinRoom(Long roomId, CustomUserDetails userDetails) {
        ReadingRoom room = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        User user = userDetails.getUser();

        // 가입 인원이 4명 이상이면 예외 발생
        int memberCount = readingRoomUserRepository.countByReadingRoom(room);
        if (memberCount >= 4) {
            throw new CustomException(ErrorCode.ROOM_CAPACITY_EXCEEDED);
        }

        // 중복 가입 방지
        boolean alreadyJoined = readingRoomUserRepository.existsByReadingRoomAndUser(room, user);
        if (alreadyJoined) {
            throw new CustomException(ErrorCode.ALREADY_JOINED_READING_ROOM);
        }

        // DB 저장
        ReadingRoomUser userEntry = ReadingRoomUser.builder()
                .readingRoom(room)
                .user(user)
                .role(Role.GUEST)
                .build();
        readingRoomUserRepository.save(userEntry);

        // Redis에 저장
        redisTemplate.opsForSet().add("ReadingRoom:" + roomId + ":joinedUsers", user.getUserId().toString());

        return roomId;
    }

}
