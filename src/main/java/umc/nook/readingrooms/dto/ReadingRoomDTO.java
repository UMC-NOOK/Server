package umc.nook.readingrooms.dto;

import lombok.*;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;

import java.util.List;

public class ReadingRoomDTO {

    public enum ReadingRoomEventType {
        BGM_TOGGLE,     // BGM 토글
        USER_ENTER,     // 사용자 입장
        USER_LEAVE,     // 사용자 퇴장
        ROOM_INFO_UPDATE, // 리딩룸 정보 수정
        ROOM_REMOVED //리딩룸 삭제
    }

    @Getter
    @Builder
    public static class ReadingRoomResponseDTO {
        private Long roomId;
        private String name;
        private String description;
        private List<String> hashtags;
        private int currentUserCount;
        private int totalUserCount;
        private String themeImageUrl;
    }

    @Getter
    @Setter
    @Builder
    public static class ReadingRoomRequestDTO {
        private String name;
        private String description;
        private List<String> hashtags;
        private Long themeId;
    }

    @Getter
    @Builder
    public static class ReadingRoomThemeUpdateDTO {
        private Long roomId;
        private String imageUrl;
        private String bgmUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingRoomEnterRequest {
        private Long roomId;
        private Long userId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingRoomEnterResponse {
        private Long roomId;
        private String imageUrl;
        private String bgmUrl;
        private boolean bgmEnabled;
        private List<UserDTO> currentUsers;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingRoomBgmToggleRequest {
        private Long roomId;
        private Long userId;
        private boolean bgmOn;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {

        private Long userId;
        private String nickname;
        private String characterColor;

        public static UserDTO from(User user) {
            return UserDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .characterColor(user.getCharacterColor().name())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserEventPayload {
        private Long userId;
        private String nickname;
        private String characterColor;
        private List<UserDTO> currentUsers;
    }
}
