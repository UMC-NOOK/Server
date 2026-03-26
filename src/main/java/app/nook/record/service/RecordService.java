package app.nook.record.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.FileErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.record.repository.RecordRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final RecordRepository recordRepository;
    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final PresignedUrlService presignedUrlService;

    // 기록 최대 개수 - 임의로 설정
    // TODO : 기록 최대개수 추후 반영
    private final int MAX_RECORD_COUNT = 1000;
    private final RecordImageRepository recordImageRepository;

    // 기록 생성
    @Transactional
    public void createRecord(
            User user,
            Long bookId,
            RecordRequestDto requestDto
    ) {
        List<String> imageKeys = sanitizeImageKeys(requestDto.imageKeys());

        // 책 존재 여부 확인
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        // 서재에 등록된 책인지 확인
        Library library = libraryRepository.findByUserAndBook(user,book);
        if (library == null )
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);

        // 동일 서재 집계 단위 잠금, create를 같은 트랜잭션에서 수행
        libraryRepository.findByIdAndUserIdForUpdate(library.getId(), user.getId())
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 최대 개수를 넘지 않는지 확인
        long count = recordRepository.countByLibraryIdAndUserId(library.getId(), user.getId());
        if (count >= MAX_RECORD_COUNT) {
            throw new CustomException(FileErrorCode.FILE_NUM_EXCEEDED);
        }

        // 기록 생성
        Record newRecord = new Record(
                library,
                requestDto.emotion(),
                requestDto.content()
        );

        recordRepository.save(newRecord);

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
            List<String> requestedImageKeys = sanitizeImageKeys(requestDto.imageKeys());

            // 기록 존재 여부 확인
            Record record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));

            // 기록이 속한 서재가 사용자에게 속한 서재인지 확인
            if (!record.getLibrary().getUser().getId().equals(user.getId())) {
                throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
            }

            // 기록 내용 업데이트
            record.update(requestDto.content(), requestDto.emotion());

            // 이미지 동기화 - 기존 이미지와 요청된 이미지 키를 비교하여 삭제 및 추가 처리
            syncRecordImages(record, requestedImageKeys);
    }

    // 기록 삭제
    @Transactional
    public void deleteRecord(
            User user,
            Long recordId
    ) {
        // 기록 존재 여부 확인
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));
        // 기록 소유자 확인
        if (!record.getLibrary().getUser().getId().equals(user.getId())) {
            throw new CustomException(RecordErrorCode.RECORD_NOT_AUTHORIZED);
        }
        record.getImages().stream()
                .map(RecordImage::getKey)
                .filter(Objects::nonNull)
                .forEach(presignedUrlService::deleteFile);
        record.getImages().clear();
        recordRepository.delete(record);
    }


    /* 기록 조회 - 전체 */

    // 기록 전체 개수 조회
    public RecordResponseDto.RecordCountDto countRecords(Long userId) {
        int recordCount = recordRepository.countByUserId(userId);
        return new RecordResponseDto.RecordCountDto(recordCount);
    }

    private List<String> sanitizeImageKeys(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return List.of();
        }

        return imageKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .toList();
    }

    // 이미지 저장 - 기록 생성, 수정 공통
    private void saveRecordImages(Record record, List<String> imageKeys) {
        for (int index = 0; index < imageKeys.size(); index++) {
            recordImageRepository.save(new RecordImage(
                    record,
                    imageKeys.get(index),
                    index
            ));
        }
    }

    private void syncRecordImages(Record record, List<String> requestedImageKeys) {
        List<RecordImage> existingImages = new ArrayList<>(record.getImages());

        // 기존 이미지 중 요청된 이미지 키에 없는 것을 삭제 - 버킷,DB에 모두 반영
        existingImages.stream()
                .filter(recordImage -> !requestedImageKeys.contains(recordImage.getKey()))
                .forEach(recordImage -> {
                    if (recordImage.getKey() != null) {
                        presignedUrlService.deleteFile(recordImage.getKey());
                    }
                    record.getImages().remove(recordImage);
                    recordImageRepository.delete(recordImage);
                });

        // 기록의 이미지 삭제
        record.getImages().clear();

        // 새로운 이미지 저장 - 순서도 반영해서 저장
        saveRecordImages(record, requestedImageKeys);
    }

    // 기록 전체 정렬 조회 - 무한스크롤

    /* 기록 조회 - 상세 */


    // 책별 기록 리스트 조회 - 감정 or 전체 필터

    // 기록 상세 조회


}
