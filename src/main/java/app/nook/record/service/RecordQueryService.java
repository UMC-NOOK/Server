package app.nook.record.service;

import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.global.response.CommonErrorCode;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.converter.RecordConverter;
import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;
import app.nook.record.repository.RecordRepository;
import app.nook.record.util.RecordListCursorCodec;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordQueryService {

    private final RecordRepository recordRepository;
    private final RecordConverter recordConverter;
    private final PresignedUrlService presignedUrlService;
    private final BookRepository bookRepository;
    private final LibraryRepository libraryRepository;


    // 독서 기록 목록 조회
    // 최신 기록 순, 오래된 기록 순, 기록 많은 순, 기록 적은 순, 커서페이징. 첫번째 커서는 Null
    public CursorResponse<BookRecordDto.BookRecordItemDto, String> getUserRecords(
            User user,
            int size,
            RecordListCursor cursor,
            SortType sortType
    ) {
        List<BookRecordDto.BookRecordItemDto> recordItems = recordRepository.findRecordsByCursor(
                user.getId(),
                cursor,
                sortType,
                size
        );

        boolean hasNext = recordItems.size() > size;

        List<BookRecordDto.BookRecordItemDto> pageContent = hasNext
                ? recordItems.subList(0, size)
                : recordItems;

        // 다음 커서는 마지막 항목의 id, 다음 커서가 없으면 null
        RecordListCursor nextCursor = hasNext
                ? toNextCursor(pageContent.get(pageContent.size() - 1), sortType)
                : null;

        List<BookRecordDto.BookRecordItemDto> resolvedPageContent = pageContent.stream()
                .map(item -> new BookRecordDto.BookRecordItemDto(
                        item.bookId(),
                        item.title(),
                        item.author(),
                        item.recordContent(),
                        presignedUrlService.resolveImageUrl(user.getId(), item.coverImageUrl()),
                        item.recordCount(),
                        item.lastCreatedDate()
                ))
                .toList();

        return CursorResponse.of(resolvedPageContent, RecordListCursorCodec.encode(nextCursor), hasNext);

    }

    // 독서 기록 감상별 개수 조회
    public BookRecordDto.RecordEmotionCountResponse getRecordEmotionCounts(User user) {
        return recordRepository.countRecordsByEmotion(user.getId());
    }

    // 해당 책의 독서 기록 목록 조회
    public CursorResponse<BookRecordDto.RecordItemDto, Long> getBookRecords(
            User user,
            Long bookId,
            int size,
            Long cursor,
            String emotion
    ) {
        if (!bookRepository.existsById(bookId)) {
            throw new CustomException(BookErrorCode.BOOK_NOT_FOUND);
        }

        if (!libraryRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        Emotion emotionFilter = parseEmotionFilter(emotion);

        List<Record> records = recordRepository.findBookRecordsByCursor(
                user.getId(),
                bookId,
                cursor,
                emotionFilter,
                size
        );

        boolean hasNext = records.size() > size;

        List<Record> pageContent = hasNext
                ? records.subList(0, size)
                : records;

        Long nextCursor = hasNext
                ? pageContent.get(pageContent.size() - 1).getId()
                : null;

        List<BookRecordDto.RecordItemDto> items = pageContent.stream()
                .map(record -> recordConverter.toRecordItemDto(user.getId(), record))
                .toList();

        return CursorResponse.of(items, nextCursor, hasNext);
    }

    private RecordListCursor toNextCursor(BookRecordDto.BookRecordItemDto item, SortType sortType) {
        return switch (sortType) {
            case RECENT_RECORDED, OLDEST_RECORDED -> new RecordListCursor(
                    null,
                    item.bookId(),
                    item.lastCreatedDate()
            );
            case RECORD_COUNT_ASC, RECORD_COUNT_DESC -> new RecordListCursor(
                    item.recordCount(),
                    item.bookId(),
                    null
            );
        };
    }

    private Emotion parseEmotionFilter(String emotion) {
        if (emotion == null || emotion.isBlank() || "ALL".equalsIgnoreCase(emotion)) {
            return null;
        }

        try {
            return Emotion.valueOf(emotion.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
    }

}
