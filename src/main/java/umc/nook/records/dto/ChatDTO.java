package umc.nook.records.dto;


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
        private Long bookId;
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
        private String message;
        private ChatType chatType;
        private LocalDateTime createdDate;

        public ChatResponseDTO(ChatRecord record){
            this.message = record.getContent();
            this.createdDate = record.getCreatedDate();
            this.chatType = record.getRole();
        }
    }

}
