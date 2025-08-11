package umc.nook.readingrooms.dto;

import lombok.*;
import umc.nook.readingrooms.domain.Role;
import umc.nook.readingrooms.domain.ThemeName;
import umc.nook.users.domain.User;

import java.util.List;

public class ReadingRoomDTO {

    public enum ReadingRoomEventType {
        BGM_TOGGLE,     // BGM 토글
        USER_ENTER,     // 사용자 입장
        USER_LEAVE,     // 사용자 퇴장
        ROOM_INFO_UPDATE, // 리딩룸 정보 수정
        ROOM_REMOVED, //리딩룸 삭제
        READING_BOOKS //독서중인 책
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
        private String alias;
        private String characterColor;

        public static UserDTO from(User user) {
            return UserDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .alias(user.getProfile().getAlias())
                    .characterColor(user.getProfile().getCharacterColor().name())
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
        private String alias;
        private String characterColor;
        private List<UserDTO> currentUsers;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingRoomLeaveRequest {
        private Long roomId;
        private Long userId;
    }

    @Getter
    @AllArgsConstructor
    public static class ReadingBookRequest {
        private Long bookId;
        private String title;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingBookPayload {
        private Long roomId;
        private Long userId;
        private Long bookId;
    }

    @Getter
    @Builder
    public static class JoinedUsersResponseDTO{
        private Long userId;
        private String nickname;
        private Role role;
        private Boolean isMe;
    }

    @Getter
    @Builder
    public static class LastAccessedReadingRoomResponseDTO {
        private Long roomId;
        private String name;
        private String description;
        private int currentUserCount;
        private String themeImageUrl;
    }

    @Getter
    @Builder
    public static class ReadingRoomThemeResponseDTO {
        private Long roomId;
        private ThemeName themeName;
        private String imageUrl;
        private String bgmUrl;
    }
}
