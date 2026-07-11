package app.nook.record.dto;

import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.record.domain.enums.Emotion;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookRecordDto(

) {
    public record BookRecordItemDto(
            Long bookId,
            String title,
            String author,
            String recordContent,
            String coverImageUrl,
            Long recordCount,
            @JsonIgnore LocalDateTime lastCreatedDate
    ){}

    public record RecordItemDto(
            Long recordId,
            String content,
            List<String> imgUrls,
            List<String> imageKeys,
            Emotion emotion,
            LocalDate createdDate
    ){}

    public record RecordEmotionDto(
            String emotion,
            long recordCount
    ){}

    public record RecordEmotionCountResponse(
            Long totalCount,                        // 전체
            List<RecordEmotionDto> emotionCounts    // 감정별
    ) {}



}
