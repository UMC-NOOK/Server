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
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.event.RecordDeletedEvent;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.record.repository.RecordRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    // TODO: 기록 최대개수 임의설정, 추후 변경
    private static final int MAX_RECORD_COUNT = 1000;

    private final RecordRepository recordRepository;
    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final RecordImageRepository recordImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 기록 생성
    @Transactional
    public void createRecord(
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
        Library library = libraryRepository.findByUserAndBook(user, book);
        if (library == null) {
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        // 서재 최대 개수를 넘기지 않도록 생성 요청 충돌 방지를 위해 낙관적 락이 걸리도록 설정
        libraryRepository.findByIdAndUserIdForUpdate(library.getId(), user.getId())
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 개수를 초과한다면 에러 반환
        long count = recordRepository.countByLibraryIdAndUserId(library.getId(), user.getId());
        if (count >= MAX_RECORD_COUNT) {
            throw new CustomException(FileErrorCode.FILE_NUM_EXCEEDED);
        }

        Record newRecord = new Record(
                library,
                requestDto.emotion(),
                requestDto.content()
        );

        // 레코드 생성
        recordRepository.save(newRecord);

        // 이미지가 있다면 key와 record로 Record 이미지 생성
        if (!imageKeys.isEmpty()) {
            saveRecordImages(newRecord, imageKeys);
        }
    }

    // 기록 수정
    @Transactional
    public void updateRecord(
            User user,
            Long recordId,
            RecordUpdateRequestDto requestDto
    ) {
        List<String> requestedImageKeys = filterImageKeys(requestDto.imageKeys());

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));

        if (!record.getLibrary().getUser().getId().equals(user.getId())) {
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        record.update(requestDto.content(), requestDto.emotion());

        // 이미지 업데이트 시에 동기화 처리
        syncRecordImages(record, requestedImageKeys);
    }

    // 기록 삭제
    @Transactional
    public void deleteRecord(
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
        recordRepository.delete(record);
        eventPublisher.publishEvent(new RecordDeletedEvent(recordId, keysToDelete));
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
        List<RecordImage> existingImages = new ArrayList<>(record.getImages());

        // 삭제될 이미지들을 추출
        List<String> keysToDelete = existingImages.stream()
                .map(RecordImage::getKey)
                .filter(Objects::nonNull)
                .filter(key -> !requestedImageKeys.contains(key))
                .toList();

        // 기존 이미지 중 요청된 이미지 키에 없는 것들은 삭제
        existingImages.stream()
                .filter(recordImage -> !requestedImageKeys.contains(recordImage.getKey()))
                .forEach(recordImage -> {
                    record.getImages().remove(recordImage);
                    recordImageRepository.delete(recordImage);
                });

        // 이미지 연관관계 정리
        record.getImages().clear();

        // 이미지 저장
        saveRecordImages(record, requestedImageKeys);

        // 삭제할 이미지가 있다면 삭제 이벤트 발행
        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new RecordDeletedEvent(record.getId(), keysToDelete));
        }
    }

    // TODO: 독서 기록 상세조회

    // TODO: 독서 기록 전체 목록조회 - 정렬 포함

    // TODO: 특정 책 기록 조회 - 감정 필터



}
