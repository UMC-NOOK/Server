package umc.nook.records.dto;

import jakarta.validation.constraints.NotBlank;
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
        private String isEssay;
        private String content;
    }



    @NoArgsConstructor
    @AllArgsConstructor
    public static class GptRequestDTO {

        @NotBlank(message = "채팅 내용은 필수입니다.")
        String message;
    }

}
