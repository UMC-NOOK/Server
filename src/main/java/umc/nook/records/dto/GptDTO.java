package umc.nook.records.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class GptDTO {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class GptResponseDTO {
        private String content;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class ChatRecordDTO {
        private boolean isEssay;
        private String content;
    }



    @NoArgsConstructor
    @AllArgsConstructor
    public static class GptRequestDTO {
        String message;
    }

}
