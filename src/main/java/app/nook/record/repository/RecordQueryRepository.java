package app.nook.record.repository;

import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;

import java.util.List;

public interface RecordQueryRepository {
    public List<BookRecordDto.BookRecordItemDto> findRecordsByCursor(
            Long userId, RecordListCursor cursor, SortType sortType, int size
    );
    public List<Record> findBookRecordsByCursor(
            Long userId, Long bookId, Long cursor, Emotion emotion, int size
    );
    public BookRecordDto.RecordEmotionCountResponse countRecordsByEmotion(Long userId, Long bookId);
}
