package umc.nook.records.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.ChatRecord;
import umc.nook.records.domain.ChatType;

import java.time.LocalDateTime;

@Getter
public class ChatDTO {

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatRequestDTO {

        @NotNull(message = "bookId는 필수입니다.")
        @Positive(message = "bookId는 양수여야 합니다.")
        private Long bookId;

        @NotBlank(message = "메세지는 필수입니다.")
        private String message;
        @Builder
        public ChatRecord toEntity(ChatType recordType, UserBookShelf userBookShelf) {
            return ChatRecord.builder()
                    .role(recordType)
                    .content(message)
                    .bookshelf(userBookShelf)
                    .build();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ChatResponseDTO {
        private Long chatRecordId;
        private String message;
        private ChatType chatType;
        private LocalDateTime createdDate;

        public ChatResponseDTO(ChatRecord record){
            this.chatRecordId = record.getId();
            this.message = record.getContent();
            this.createdDate = record.getCreatedDate();
            this.chatType = record.getRole();
        }
    }

}
