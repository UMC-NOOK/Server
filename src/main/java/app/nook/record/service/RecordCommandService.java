package app.nook.record.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.FileErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.event.RecordDeletedEvent;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.record.repository.RecordRepository;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.repository.TimelineRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordCommandService {

    // TODO: 기록 최대개수 임의설정, 추후 변경
    private static final int MAX_RECORD_COUNT = 1000;

    private final RecordRepository recordRepository;
    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final RecordImageRepository recordImageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimelineCommandService timelineCommandService;
    private final TimelineRepository timelineRepository;

    // 기록 생성
    @Transactional
    public Long createRecord(
            User user,
            Long bookId,
            RecordRequestDto requestDto
    ) {
        // 이미지 키 리스트
        List<String> imageKeys = filterImageKeys(requestDto.imageKeys());

        // 책 존재 여부 확인
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        // 서재 등록 여부 확인
        Library library = libraryRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 서재 최대 개수 초과를 막기 위해 for update 기반 비관적 락을 건다.
        libraryRepository.findByIdAndUserIdForUpdate(library.getId(), user.getId())
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 개수를 초과한다면 에러 반환
        long count = recordRepository.countByLibraryIdAndUserId(library.getId(), user.getId());
        if (count >= MAX_RECORD_COUNT) {
            throw new CustomException(FileErrorCode.FILE_NUM_EXCEEDED);
        }

        Record newRecord = Record.create(
                library,
                normalizeEmotion(requestDto.emotion()),
                requestDto.content()
        );

        // 레코드 생성
        recordRepository.save(newRecord);

        // 이미지가 있다면 key와 record로 Record 이미지 생성
        if (!imageKeys.isEmpty()) {
            saveRecordImages(newRecord, imageKeys);
        }

        timelineCommandService.appendRecordCreated(newRecord, imageKeys.size());
        return newRecord.getId();
    }

    // 기록 수정
    @Transactional
    public Long updateRecord(
            User user,
            Long recordId,
            RecordUpdateRequestDto requestDto
    ) {
        List<String> requestedImageKeys = filterImageKeys(requestDto.imageKeys());

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));

        if (!record.getLibrary().getUser().getId().equals(user.getId())) {
            throw new CustomException(RecordErrorCode.RECORD_NOT_AUTHORIZED);
        }

        record.update(requestDto.content(), normalizeEmotion(requestDto.emotion()));

        // 이미지 업데이트 시에 동기화 처리
        syncRecordImages(record, requestedImageKeys);
        return record.getId();
    }

    // 기록 삭제
    @Transactional
    public Long deleteRecord(
            User user,
            Long recordId
    ) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));

        if (!record.getLibrary().getUser().getId().equals(user.getId())) {
            throw new CustomException(RecordErrorCode.RECORD_NOT_AUTHORIZED);
        }

        List<String> keysToDelete = record.getImages().stream()
                .map(RecordImage::getKey)
                .filter(Objects::nonNull)
                .toList();

        record.getImages().clear();
        timelineRepository.deleteByLibraryAndTypeAndTargetIdIn(
                record.getLibrary(), TimelineType.RECORD, List.of(recordId));
        recordRepository.delete(record);
        eventPublisher.publishEvent(new RecordDeletedEvent(recordId, keysToDelete));
        return recordId;
    }

    public RecordResponseDto.RecordCountDto countRecords(Long userId) {
        long recordCount = recordRepository.countByUserId(userId);
        return new RecordResponseDto.RecordCountDto(recordCount);
    }

    private List<String> filterImageKeys(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return List.of();
        }

        return imageKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .toList();
    }

    private Emotion normalizeEmotion(Emotion emotion) {
        return emotion == null ? Emotion.EMPTY : emotion;
    }

    // 기록 이미지 저장
    private void saveRecordImages(Record record, List<String> imageKeys) {
        for (int index = 0; index < imageKeys.size(); index++) {
            recordImageRepository.save(new RecordImage(
                    record,
                    imageKeys.get(index),
                    index
            ));
        }
    }

    // 수정 요청에 따라 동기화
    private void syncRecordImages(Record record, List<String> requestedImageKeys) {
        Map<String, RecordImage> existingImagesByKey = record.getImages().stream()
                .filter(recordImage -> recordImage.getKey() != null)
                .collect(Collectors.toMap(RecordImage::getKey, Function.identity(), (first, second) -> first));

        // 요청에서 빠진 기존 이미지는 삭제
        List<String> keysToDelete = existingImagesByKey.keySet().stream()
                .filter(key -> !requestedImageKeys.contains(key))
                .toList();
        keysToDelete.forEach(key -> {
            RecordImage recordImage = existingImagesByKey.get(key);
            record.getImages().remove(recordImage);
            recordImageRepository.delete(recordImage);
        });

        // 요청 키 중 기존에 없던 것만 새로 추가하고, 이미 있던 것은 순서만 업데이트
        for (int index = 0; index < requestedImageKeys.size(); index++) {
            String key = requestedImageKeys.get(index);
            RecordImage existingImage = existingImagesByKey.get(key);
            if (existingImage == null) {
                record.getImages().add(recordImageRepository.save(new RecordImage(record, key, index)));
            } else if (!Objects.equals(existingImage.getOrderIndex(), index)) {
                existingImage.updateOrderIndex(index);
            }
        }

        // 삭제할 이미지가 있다면 삭제 이벤트 발행
        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new RecordDeletedEvent(record.getId(), keysToDelete));
        }
    }


}
