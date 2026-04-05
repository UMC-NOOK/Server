package app.nook.record.dto;

import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.record.domain.enums.Emotion;

import java.time.LocalDate;
import java.util.List;

public record BookRecordDto(

) {
    public record BookRecordItemDto(
            Long bookId,
            String title,
            String author,
            String recordContent,
            String coverImageUrl,
            Long recordCount
    ){}

    public record RecordItemDto(
            Long recordId,
            String content,
            List<String> imgUrls,
            Emotion emotion,
            LocalDate createdDate
    ){}

    public record RecordEmotionDto(
            Emotion emotion,
            long recordCount
    ){}

    public record RecordEmotionCountResponse(
            Long totalCount,                        // 전체
            List<RecordEmotionDto> emotionCounts    // 감정별
    ) {}



}
