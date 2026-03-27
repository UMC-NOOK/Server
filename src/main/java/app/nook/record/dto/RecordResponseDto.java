package app.nook.record.dto;

public class RecordResponseDto {

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
