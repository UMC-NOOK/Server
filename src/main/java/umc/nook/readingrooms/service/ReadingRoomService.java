package umc.nook.readingrooms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.readingrooms.domain.*;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.repository.*;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingRoomService {

    private final ReadingRoomRepository readingRoomRepository;
    private final ReadingRoomUserRepository readingRoomUserRepository;
    private final ThemeRepository themeRepository;
    private final HashtagRepository hashtagRepository;
    private final ReadingRoomHashtagRepository readingRoomHashtagRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    // 전체 리딩룸 조회
    @Transactional(readOnly = true)
    public List<ReadingRoomDTO.ReadingRoomResponseDTO> getAllReadingRooms(int page) {

        int pageSize = 12;
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<ReadingRoom> readingRooms = readingRoomRepository.findAll(pageRequest);

        return readingRooms.stream().map(room -> {

            // 실시간 접속자 수는 Redis에서 조회
            String connectedKey = "ReadingRoom:" + room.getId() + ":users";
            Long connectedCount = redisTemplate.opsForSet().size(connectedKey);

            // 가입자 수는 DB에서 조회
            int joinedCount = readingRoomUserRepository.countByReadingRoom(room);

            List<String> hashtagNames = room.getHashtags().stream()
                    .map(hashtag -> hashtag.getHashtag().getName().name()) // enum이면 .name()
                    .toList();

            return ReadingRoomDTO.ReadingRoomResponseDTO.builder()
                    .roomId(room.getId())
                    .name(room.getName())
                    .description(room.getDescription())
                    .hashtags(hashtagNames)
                    .currentUserCount(connectedCount != null ? connectedCount.intValue() : 0)
                    .totalUserCount(joinedCount)
                    .themeImageUrl(room.getTheme().getImageUrl())
                    .build();

        }).collect(Collectors.toList());
    }

    //사용자가 가입한 리딩룸 조회
    @Transactional(readOnly = true)
    public List<ReadingRoomDTO.ReadingRoomResponseDTO> getJoinedReadingRooms(int page, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        int pageSize = 12;
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<ReadingRoomUser> joinedReadingRooms = readingRoomUserRepository.findByUser(user, pageRequest);

        return joinedReadingRooms.stream()
                .map(join -> {
                    ReadingRoom room = join.getReadingRoom();

                    //가입자수는 DB에서 조회
                    int joinedCount = readingRoomUserRepository.countByReadingRoom(room);

                    // 실시간 접속자 수는 Redis에서 조회
                    String connectedKey = "ReadingRoom:" + room.getId() + ":users";
                    Long connectedCount = redisTemplate.opsForSet().size(connectedKey);

                    List<String> hashtagNames = room.getHashtags().stream()
                            .map(h -> h.getHashtag().getName().name())
                            .toList();

                    return ReadingRoomDTO.ReadingRoomResponseDTO.builder()
                            .roomId(room.getId())
                            .name(room.getName())
                            .description(room.getDescription())
                            .hashtags(hashtagNames)
                            .currentUserCount(connectedCount != null ? connectedCount.intValue() : 0)
                            .totalUserCount(joinedCount)
                            .themeImageUrl(room.getTheme().getImageUrl())
                            .build();
                }).toList();
    }

    // 리딩룸 가입, DB에 저장
    @Transactional
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

        return roomId;
    }

    // 리딩룸 생성, DB에 저장
    @Transactional
    public Long createRoom(ReadingRoomDTO.ReadingRoomRequestDTO readingRoomRequestDTO, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        if (readingRoomRequestDTO.getHashtags().size() > 3) {
            throw new CustomException(ErrorCode.TOO_MANY_HASHTAGS);
        }

        Theme theme = themeRepository.findById(readingRoomRequestDTO.getThemeId())
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));

        ReadingRoom readingRoom = ReadingRoom.builder()
                .name(readingRoomRequestDTO.getName())
                .description(readingRoomRequestDTO.getDescription())
                .theme(theme)
                .build();
        readingRoomRepository.save(readingRoom);

        ReadingRoomUser readingRoomUser = ReadingRoomUser.builder()
                .readingRoom(readingRoom)
                .user(user)
                .role(Role.HOST)
                .lastAccessedAt(LocalDateTime.now())
                .build();
        readingRoomUserRepository.save(readingRoomUser);

        // TODO: 추후 해시태그 예외처리 리팩토링 예정
        List<ReadingRoomHashtag> hashtagMappings = readingRoomRequestDTO.getHashtags().stream()
                .map(name -> {
                    HashtagName hashtagName;
                    try {
                        hashtagName = HashtagName.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        throw new CustomException(ErrorCode.HASHTAG_NOT_FOUND);
                    }

                    Hashtag hashtag = hashtagRepository.findByName(hashtagName)
                            .orElseThrow(() -> new CustomException(ErrorCode.HASHTAG_NOT_FOUND));

                    return ReadingRoomHashtag.builder()
                            .readingRoom(readingRoom)
                            .hashtag(hashtag)
                            .build();
                })
                .toList();
        readingRoomHashtagRepository.saveAll(hashtagMappings);

        return readingRoom.getId();
    }

    // 리딩룸 삭제
    @Transactional
    public Long deleteRoom(Long roomId, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        ReadingRoom readingRoom = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        // 해당 유저가 이 리딩룸의 HOST인지 확인
        boolean isHost = readingRoomUserRepository.existsByReadingRoomAndUserAndRole(readingRoom, user, Role.HOST);
        if (!isHost) {
            throw new CustomException(ErrorCode.HOST_ONLY);
        }

        // 리딩룸 삭제
        readingRoomRepository.delete(readingRoom);

        // Redis 정리 (실시간 접속자 정보)
        String usersKey = "ReadingRoom:" + roomId + ":users";
        redisTemplate.delete(usersKey);

        // WebSocket broadcast
        messagingTemplate.convertAndSend("/readingroom/sub/removed", roomId);

        // 삭제된 리딩룸 ID 반환
        return roomId;
    }

    @Transactional
    public void updateRoom(Long roomId, ReadingRoomDTO.ReadingRoomRequestDTO dto, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        ReadingRoom room = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        // 해당 유저가 이 리딩룸의 HOST인지 확인
        boolean isHost = readingRoomUserRepository.existsByReadingRoomAndUserAndRole(room, user, Role.HOST);
        if (!isHost) throw new CustomException(ErrorCode.HOST_ONLY);

        // 테마 변경 여부 추적
        boolean themeChanged = false;
        Theme updatedTheme = room.getTheme(); // 기본값은 현재 테마

        // 이름 수정
        if (dto.getName() != null) {
            room.updateName(dto.getName());
        }

        // 설명 수정
        if (dto.getDescription() != null) {
            room.updateDescription(dto.getDescription());
        }

        // 테마 수정
        if (dto.getThemeId() != null && !dto.getThemeId().equals(room.getTheme().getId())) {
            Theme newTheme = themeRepository.findById(dto.getThemeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));
            room.updateTheme(newTheme);
            updatedTheme = newTheme;
            themeChanged = true;
        }

        // 해시태그 수정
        if (dto.getHashtags() != null) {
            // 기존 해시태그 매핑 제거
            readingRoomHashtagRepository.deleteByReadingRoom(room);

            // 최대 3개까지 등록
            List<ReadingRoomHashtag> newMappings = dto.getHashtags().stream()
                    .limit(3)
                    .map(name -> {
                        HashtagName hashtagName;
                        try {
                            hashtagName = HashtagName.valueOf(name);
                        } catch (IllegalArgumentException e) {
                            throw new CustomException(ErrorCode.HASHTAG_NOT_FOUND);
                        }

                        Hashtag hashtag = hashtagRepository.findByName(hashtagName)
                                .orElseThrow(() -> new CustomException(ErrorCode.HASHTAG_NOT_FOUND));

                        return ReadingRoomHashtag.builder()
                                .readingRoom(room)
                                .hashtag(hashtag)
                                .build();
                    })
                    .toList();

            readingRoomHashtagRepository.saveAll(newMappings);
        }

        // WebSocket broadcast - 테마가 변경된 경우에만
        if (themeChanged) {
            messagingTemplate.convertAndSend("/readingroom/sub/updated-theme",
                    ReadingRoomDTO.ReadingRoomThemeUpdateDTO.builder()
                            .roomId(room.getId())
                            .imageUrl(updatedTheme.getImageUrl())
                            .bgmUrl(updatedTheme.getBgmUrl())
                            .build()
            );
        }
    }
}
