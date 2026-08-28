package app.nook.record.dto;

public class RecordResponseDto {

    public record RecordIdDto(
            Long recordId
    ) {
    }

    public record RecordDetailDto(
            Long recordId,
            String content,
            String emotion,
            String imageUrl
    ) {
    }

    public record RecordCountDto(
            long count
    ) {
    }


}
