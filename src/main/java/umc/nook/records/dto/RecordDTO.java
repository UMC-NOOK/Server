package umc.nook.records.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.domain.BookRecord;
import umc.nook.records.domain.ChatRecord;
import umc.nook.records.domain.RecordType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class RecordDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordRequestDTO {
        private Long bookId;
        private String page;
        private String content;
        @Builder
        public BookRecord toEntity(UserBookShelf userBook) {
            return BookRecord.builder()
                    .bookshelf(userBook)
                    .content(content)
                    .page(page)
                    .build();
        }
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordUpdateRequestDTO {
        private Long recordId;
        private String page;
        private String content;

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentUpdateRequestDTO {
        private Long commentId;
        private String content;
    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentRequestDTO {

        private Long bookId;

        @Schema(description = "부모 문장 ID (문장에 감상을 달 경우에 명시, 아닐 경우 null)", example = "1", nullable = true)
        private Long parentRecordId;
        private String content;

        public BookRecord toEntity(UserBookShelf userBookShelf, BookRecord parent) {
            return BookRecord.builder()
                    .content(content)
                    .page(null)
                    .bookshelf(userBookShelf)
                    .parent(parent)
                    .build();
        }

    }


    @Getter
    @AllArgsConstructor
    public static class RecordResponseDTO {
        private Long recordId;
        private String page;
        private String content;
        private RecordType recordType;
        private LocalDateTime createdDate;

        private List<CommentResponseDTO> comments;

        public RecordResponseDTO(BookRecord record) {
            this.recordId = record.getId();
            this.page = record.getPage();
            this.content = record.getContent();
            this.recordType = record.getRecordType();
            this.createdDate = record.getCreatedDate();

            // 자식이 댓글일 경우만 수집
            this.comments = record.getComments() != null
                    ? record.getComments().stream()
                    .filter(child -> child.getRecordType() == RecordType.COMMENTARY)
                    .map(CommentResponseDTO::new)
                    .collect(Collectors.toList())
                    : List.of();
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SentenceResponseDTO {
        private Long sentenceId;
        private String page;
        private String content;
        private LocalDateTime createdDate;

        public SentenceResponseDTO(BookRecord sentence) {
            this.sentenceId = sentence.getId();
            this.page = sentence.getPage();
            this.content = sentence.getContent();
            this.createdDate = sentence.getCreatedDate();
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentResponseDTO {
        private Long commentId;
        private String content;
        private LocalDateTime createdDate;

        public CommentResponseDTO(BookRecord comment) {
            this.commentId = comment.getId();
            this.content = comment.getContent();
            this.createdDate = comment.getCreatedDate();
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyRecordRateResponseDTO {
        private List<MonthRate> rates;

        @Getter
        @AllArgsConstructor
        public static class MonthRate {
            private int month; // 1 ~ 12
            private double rate;
        }
    }


}
