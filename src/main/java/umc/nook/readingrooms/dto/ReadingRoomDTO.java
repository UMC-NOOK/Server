package umc.nook.readingrooms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;

import java.util.List;

public class ReadingRoomDTO {

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
    @Builder
    public static class ReadingRoomEnterRequest {
        private Long roomId;
        private Long userId;
    }

    @Getter
    @Builder
    public static class ReadingRoomEnterResponse {
        private Long roomId;
        private String imageUrl;
        private String bgmUrl;
        private boolean bgmEnabled;
        private List<UserDTO> currentUsers;
    }

    @Getter
    @Builder
    public static class ReadingRoomBgmToggleRequest {
        private Long roomId;
        private Long userId;
        private boolean bgmOn;
    }

    @Getter
    @Builder
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

    @Getter
    @Builder
    public static class UserJoinBroadcast {
        private Long roomId;
        private List<ReadingRoomDTO.UserDTO> currentUsers;
    }

}
