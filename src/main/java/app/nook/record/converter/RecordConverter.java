package app.nook.record.converter;

import app.nook.r2.service.PresignedUrlService;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.dto.BookRecordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RecordConverter {

    private final PresignedUrlService presignedUrlService;

    public BookRecordDto.RecordItemDto toRecordItemDto(Long userId, Record record) {
        List<String> imageUrls = record.getImages().stream()
                .sorted(Comparator
                        .comparing(RecordImage::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RecordImage::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(RecordImage::getKey)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .map(key -> presignedUrlService.getImageUrl(userId, key))
                .toList();

        return new BookRecordDto.RecordItemDto(
                record.getId(),
                record.getContent(),
                imageUrls,
                record.getEmotion(),
                record.getCreatedDate().toLocalDate()
        );
    }
}
