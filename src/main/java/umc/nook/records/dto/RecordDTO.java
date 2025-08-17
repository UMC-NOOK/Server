package umc.nook.records.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
        @NotNull(message = "bookId는 필수입니다.")
        @Schema(description = "책 ID", example = "42", required = true)
        @Positive(message = "bookId는 양수여야 합니다.")
        private Long bookId;

        @Schema(description = "페이지 번호(양수 정수)", example = "123", required = true, minimum = "1")
        @NotNull(message = "page는 필수입니다.")
        @Positive(message = "page는 양수여야 합니다.")
        private String page;

        @Schema(description = "기록할 문장 내용(1~300자)", example = "인상 깊은 문장을 메모합니다.", maxLength = 300)
        @NotBlank(message = "content는 필수입니다.")
        @Size(min = 1, max = 300, message = "content는 1자 이상 300자 이하로 입력해야 합니다.")
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
        @Schema(description = "기존 문장 레코드 ID", example = "1001", required = true)
        @NotNull(message = "recordId는 필수입니다.")
        @Positive(message = "recordId는 양수여야 합니다.")
        private Long recordId;

        @NotBlank(message = "page는 필수입니다.")
        @Positive(message = "페이지는 양수여야 합니다.")
        @Pattern(regexp = "^[0-9]+$", message = "page는 숫자만 입력 가능합니다.")
        @Schema(description = "수정할 페이지 번호(양수 정수)", example = "234", required = true, minimum = "1")
        private String page;

        @Schema(description = "수정할 내용(1~300자)", example = "표현을 조금 다듬었습니다.", maxLength = 300)
        @NotBlank(message = "content는 필수입니다.")
        @Size(min = 1, max = 300, message = "content는 1자 이상 300자 이하로 입력해야 합니다.")
        private String content;

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentUpdateRequestDTO {

        @Schema(description = "수정하려는 감상 ID", example = "2002", required = true)
        @NotNull(message = "commentId는 필수입니다.")
        @Positive(message = "commentId는 양수여야 합니다.")
        private Long commentId;

        @Schema(description = "수정할 내용(1~300자)", example = "표현을 조금 다듬었습니다.", maxLength = 300)
        @NotBlank(message = "content는 필수입니다.")
        @Size(min = 1, max = 300, message = "content는 1자 이상 300자 이하로 입력해야 합니다.")
        private String content;
    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentRequestDTO {

        @Schema(description = "책 ID", example = "42", required = true)
        @NotNull(message = "bookId는 필수입니다.")
        @Positive(message = "bookId는 양수여야 합니다.")
        private Long bookId;

        @Schema(description = "부모 문장 ID (문장에 감상을 달 경우에 명시, 아닐 경우 null)", example = "1", nullable = true)
        private Long parentRecordId;

        @Schema(description = "감상 내용", example = "정말 인상 깊은 구절이었습니다.")
        @NotBlank(message = "내용은 필수입니다.")
        @Size(min = 1, max = 300, message = "내용은 1자 이상 300자 이하로 입력해야 합니다.")
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
