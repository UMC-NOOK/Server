package umc.nook.readingrooms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.readingrooms.domain.*;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.repository.*;
import umc.nook.users.domain.User;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingRoomService {

    private final UserRepository userRepository;
    private final ReadingRoomRepository readingRoomRepository;
    private final ReadingRoomUserRepository readingRoomUserRepository;
    private final ThemeRepository themeRepository;
    private final HashtagRepository hashtagRepository;
    private final ReadingRoomHashtagRepository readingRoomHashtagRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserBookshelfRepository userBookshelfRepository;

    //웹소켓 이벤트 발행
    private void publishWebSocketEvent(Long roomId, ReadingRoomDTO.ReadingRoomEventType eventType, Object payload) {

        String destination = "/sub/readingroom/" + roomId;
        Object actualPayloadToSend = payload;

        switch(eventType) {
            case BGM_TOGGLE:
                destination += "/bgm-toggle";
                break;
            case USER_ENTER:
                destination += "/user-enter";
                break;
            case USER_LEAVE:
                destination += "/user-leave";
                break;
            case ROOM_INFO_UPDATE:
                destination += "/room-info-update";
                break;
            case ROOM_REMOVED:
                destination += "/room-removed";
                break;
            case READING_BOOKS:
                destination += "/reading-books";
                break;
            default:
                log.warn("Unhandled event type for WebSocket publishing: {}", eventType);
        }

        messagingTemplate.convertAndSend(destination, actualPayloadToSend);
        log.info("Published WebSocket event to {}: {} with payload type {}", destination, eventType, payload.getClass().getSimpleName());

    }

    // 전체 리딩룸 조회
    @Transactional(readOnly = true)
    public List<ReadingRoomDTO.ReadingRoomResponseDTO> getAllReadingRooms(int page) {

        int pageSize = 12;
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<ReadingRoom> readingRooms = readingRoomRepository.findAll(pageRequest);

        return readingRooms.stream().map(room -> {

            // 실시간 접속자 수는 Redis에서 조회
            String hashKey = "ReadingRoom:Users:" + room.getId();
            Long connectedCount = redisTemplate.opsForHash().size(hashKey);

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
                    String hashKey = "ReadingRoom:Users:" + room.getId();
                    Long connectedCount = redisTemplate.opsForHash().size(hashKey);

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

        int MAX_NUMBER = 4;

        // 가입 인원이 4명 이상이면 예외 발생
        int memberCount = readingRoomUserRepository.countByReadingRoom(room);
        if (memberCount >= MAX_NUMBER) {
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
    public Long createRoom(ReadingRoomDTO.ReadingRoomRequestDTO dto, ThemeName themeName, List<HashtagName> hashtags, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        if (hashtags.size() > 3) {
            throw new CustomException(ErrorCode.TOO_MANY_HASHTAGS);
        }

        Theme theme = themeRepository.findByName(themeName)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));

        ReadingRoom readingRoom = ReadingRoom.builder()
                .name(dto.getName())
                .description(dto.getDescription())
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

        List<ReadingRoomHashtag> hashtagMappings = hashtags.stream()
                .map(name -> {
                    Hashtag hashtag = hashtagRepository.findByName(name)
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

    // 리딩룸 HOST인지 GUEST인지 확인
    @Transactional(readOnly = true)
    public String getUserRoleInRoom(Long roomId, CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        ReadingRoom room = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        return readingRoomUserRepository.findByReadingRoomAndUser(room, user)
                .map(readingRoomUser -> readingRoomUser.getRole().name())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_JOINED_ROOM));
    }

    // HOST: 리딩룸 삭제
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
        String usersHashKey = "ReadingRoom:Users:" + roomId;
        redisTemplate.delete(usersHashKey);

        // WebSocket broadcast
        publishWebSocketEvent(roomId, ReadingRoomDTO.ReadingRoomEventType.ROOM_REMOVED, roomId);

        // 삭제된 리딩룸 ID 반환
        return roomId;
    }

    //GUEST: 리딩룸 삭제하지 않고 탈퇴만
    @Transactional
    public Long leaveRoom(Long roomId, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        ReadingRoom readingRoom = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        ReadingRoomUser readingRoomUser = readingRoomUserRepository.findByReadingRoomAndUser(readingRoom, user)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_JOINED_ROOM));

        readingRoomUserRepository.delete(readingRoomUser);
        return roomId;
    }

    // 리딩룸 정보 수정 - 테마 변경시에만 WebSocket broadcast
    @Transactional
    public void updateRoom(Long roomId, ReadingRoomDTO.ReadingRoomRequestDTO dto, ThemeName themeName, List<HashtagName> hashtags ,CustomUserDetails userDetails) {

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
        if (themeName != null && !themeName.equals(room.getTheme().getName())) {
            Theme newTheme = themeRepository.findByName(themeName)
                    .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));
            room.updateTheme(newTheme);
            updatedTheme = newTheme;
            themeChanged = true;
        }

        // 해시태그 수정
        if (hashtags != null) {
            readingRoomHashtagRepository.deleteByReadingRoom(room);

            List<ReadingRoomHashtag> newMappings = hashtags.stream()
                    .limit(3)
                    .map(name -> {
                        Hashtag hashtag = hashtagRepository.findByName(name)
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
            ReadingRoomDTO.ReadingRoomThemeUpdateDTO payload = ReadingRoomDTO.ReadingRoomThemeUpdateDTO.builder()
                    .roomId(room.getId())
                    .imageUrl(updatedTheme.getImageUrl())
                    .bgmUrl(updatedTheme.getBgmUrl())
                    .build();
            publishWebSocketEvent(room.getId(), ReadingRoomDTO.ReadingRoomEventType.ROOM_INFO_UPDATE, payload);
        }
    }

    // 리딩룸 입장
    @Transactional
    public void enterRoom(ReadingRoomDTO.ReadingRoomEnterRequest request) {

        // 리딩룸 존재 확인
        ReadingRoom room = readingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        // 유저 존재 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저가 리딩룸에 가입되어 있는지 확인
        ReadingRoomUser readingRoomUser = readingRoomUserRepository.findByReadingRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_JOINED_ROOM));

        // 입장 시점 기록 (lastAccessedAt 갱신)
        readingRoomUser.updateLastAccessedAt();

        // Redis에 JSON으로 사용자 정보 저장
        String hashKey = "ReadingRoom:Users:" + room.getId();
        String userIdStr = String.valueOf(user.getUserId());

        if (!redisTemplate.opsForHash().hasKey(hashKey, userIdStr)) {
            try {
                ReadingRoomDTO.UserDTO dtoToStore = ReadingRoomDTO.UserDTO.from(user);
                String json = objectMapper.writeValueAsString(dtoToStore);
                redisTemplate.opsForHash().put(hashKey, userIdStr, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Redis 저장 중 JSON 직렬화 오류", e);
            }
        }

        // Redis에서 전체 유저 JSON 가져오기 (새로 입장한 사용자 포함)
        Map<Object, Object> redisUserMap = redisTemplate.opsForHash().entries(hashKey);
        List<ReadingRoomDTO.UserDTO> currentUsers = redisUserMap.values().stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue((String) json, ReadingRoomDTO.UserDTO.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON parsing error", e);
                    }
                }).toList();

        // WebSocket broadcast - 입장한 사용자
        ReadingRoomDTO.UserEventPayload userEnterEventPayload = ReadingRoomDTO.UserEventPayload.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .alias(user.getProfile().getAlias())
                .characterColor(user.getProfile().getCharacterColor().name())
                .currentUsers(currentUsers)
                .build();
        publishWebSocketEvent(room.getId(), ReadingRoomDTO.ReadingRoomEventType.USER_ENTER, userEnterEventPayload);
    }

    // 호스트가 리딩룸 전체 bgm 토글
    @Transactional
    public void toggleBgm(ReadingRoomDTO.ReadingRoomBgmToggleRequest dto) {

        ReadingRoom room = readingRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 HOST인지 확인
        boolean isHost = readingRoomUserRepository.existsByReadingRoomAndUserAndRole(room, user, Role.HOST);
        if (!isHost) {
            throw new CustomException(ErrorCode.HOST_ONLY);
        }

        // BGM 토글
        room.toggleBgm(dto.isBgmOn());

        // WebSocket broadcast - bgm 토글 boolean값
        ReadingRoomDTO.ReadingRoomBgmToggleRequest payload = ReadingRoomDTO.ReadingRoomBgmToggleRequest.builder()
                .roomId(dto.getRoomId())
                .userId(dto.getUserId())
                .bgmOn(dto.isBgmOn())
                .build();
        publishWebSocketEvent(dto.getRoomId(), ReadingRoomDTO.ReadingRoomEventType.BGM_TOGGLE, payload);
    }

    // 리딩룸 퇴장
    @Transactional
    public void leaveRoom(ReadingRoomDTO.ReadingRoomLeaveRequest dto) {

        // 리딩룸 존재 확인
        ReadingRoom room = readingRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        // 유저 존재 확인
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저가 리딩룸에 가입되어 있는지 확인
        readingRoomUserRepository.findByReadingRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_JOINED_ROOM));

        // Redis에서 사용자 제거
        String hashKey = "ReadingRoom:Users:" + dto.getRoomId();
        String userIdStr = String.valueOf(dto.getUserId());
        redisTemplate.opsForHash().delete(hashKey, userIdStr);

        // Redis에서 현재 남아있는 사용자 목록 가져오기
        Map<Object, Object> redisUserMap = redisTemplate.opsForHash().entries(hashKey);
        List<ReadingRoomDTO.UserDTO> currentUsers = redisUserMap.values().stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue((String) json, ReadingRoomDTO.UserDTO.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON parsing error", e);
                    }
                }).toList();

        // WebSocket broadcast - 퇴장한 사용자
        ReadingRoomDTO.UserEventPayload payload = ReadingRoomDTO.UserEventPayload.builder()
                .userId(dto.getUserId())
                .nickname(user.getNickname())
                .characterColor(user.getProfile().getCharacterColor().name())
                .currentUsers(currentUsers)
                .build();
        publishWebSocketEvent(room.getId(), ReadingRoomDTO.ReadingRoomEventType.USER_LEAVE, payload);
    }

    // 사용자가 독서중인 책 제목 조회
    @Transactional(readOnly = true)
    public List<ReadingRoomDTO.ReadingBookRequest> getReadingBooksInRoom(CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        return userBookshelfRepository.findByUserAndReadingStatus(user, ReadingStatus.READING)
                .stream()
                .map(shelf -> new ReadingRoomDTO.ReadingBookRequest(
                        shelf.getBook().getBookId(),      // bookId
                        shelf.getBook().getTitle()    // title
                ))
                .collect(Collectors.toList());
    }

    //독서중인 책 설정
    @Transactional
    public void readingBooks(ReadingRoomDTO.ReadingBookPayload payload) {
        // WebSocket broadcast - 독서중인 책 제목
        publishWebSocketEvent(payload.getRoomId(), ReadingRoomDTO.ReadingRoomEventType.READING_BOOKS, payload);
    }

    //리딩룸에 가입한 사용자 조회
    @Transactional
    public List<ReadingRoomDTO.JoinedUsersResponseDTO> getJoinedUsersInRoom(Long roomId, CustomUserDetails currentUser) {

        User me = currentUser.getUser(); // 현재 로그인한 유저

        //리딩룸 존재하는지 확인
        ReadingRoom room = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        List<ReadingRoomUser> joinedUsers = readingRoomUserRepository.findAllByReadingRoom(room);

        return joinedUsers.stream()
                .map(joined -> ReadingRoomDTO.JoinedUsersResponseDTO.builder()
                        .userId(joined.getUser().getUserId())
                        .nickname(joined.getUser().getNickname())
                        .role(joined.getRole())
                        .isMe(joined.getUser().getUserId().equals(me.getUserId()))
                        .build())
                .collect(Collectors.toList());
    }

    // 최근 접속한 리딩룸 조회
    @Transactional(readOnly = true)
    public ReadingRoomDTO.LastAccessedReadingRoomResponseDTO getLastAccessedRoom(User user) {
        ReadingRoomUser recent = readingRoomUserRepository
                .findTopByUserOrderByLastAccessedAtDesc(user)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        ReadingRoom room = recent.getReadingRoom();

        String hashKey = "ReadingRoom:Users:" + room.getId();
        Long connectedCount = redisTemplate.opsForHash().size(hashKey);

        return ReadingRoomDTO.LastAccessedReadingRoomResponseDTO.builder()
                .roomId(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .currentUserCount(connectedCount != null ? connectedCount.intValue() : 0)
                .themeImageUrl(room.getTheme().getImageUrl())
                .build();
    }

    // 리딩룸 테마 조회
    @Transactional(readOnly = true)
    public ReadingRoomDTO.ReadingRoomThemeResponseDTO getRoomTheme(Long roomId) {

        ReadingRoom room = readingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.READING_ROOM_NOT_FOUND));

        return ReadingRoomDTO.ReadingRoomThemeResponseDTO.builder()
                .roomId(room.getId())
                .themeName(room.getTheme().getName())
                .imageUrl(room.getTheme().getImageUrl())
                .bgmUrl(room.getTheme().getBgmUrl())
                .build();
    }
}

