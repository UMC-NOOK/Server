package umc.nook.readingrooms.dto;

import lombok.Builder;
import lombok.Getter;

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
}
