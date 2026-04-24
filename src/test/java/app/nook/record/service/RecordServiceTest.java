package app.nook.record.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.BookFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.global.response.FileErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.event.RecordDeletedEvent;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.record.repository.RecordRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordService 테스트")
class RecordServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordImageRepository recordImageRepository;

    @InjectMocks
    private RecordCommandService recordService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TimelineCommandService timelineCommandService;

    @Nested
    @DisplayName("기록 등록")
    class CreateRecord {

        @Test
        @DisplayName("성공 - 이미지 없이 기록을 생성한다")
        void 기록_생성_성공_이미지없음() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, List.of());

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(libraryRepository.findByIdAndUserIdForUpdate(20L, 1L)).willReturn(Optional.of(library));
            given(recordRepository.countByLibraryIdAndUserId(20L, 1L)).willReturn(0L);

            // when
            recordService.createRecord(user, 10L, request);

            // then
            verify(recordRepository).save(any(Record.class));
            verify(recordImageRepository, never()).save(any());
            verify(timelineCommandService).appendRecordCreated(any(Record.class), eq(0));
        }

        @Test
        @DisplayName("성공 - 이미지와 함께 기록을 생성한다")
        void 기록_생성_성공_이미지있음() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            List<String> imageKeys = List.of("record/users/1/a.png", "record/users/1/b.png");
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.USEFUL, imageKeys);

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(libraryRepository.findByIdAndUserIdForUpdate(20L, 1L)).willReturn(Optional.of(library));
            given(recordRepository.countByLibraryIdAndUserId(20L, 1L)).willReturn(0L);

            // when
            recordService.createRecord(user, 10L, request);

            // then
            verify(recordRepository).save(any(Record.class));
            verify(recordImageRepository, times(2)).save(any(RecordImage.class));
            verify(timelineCommandService).appendRecordCreated(any(Record.class), eq(2));
        }

        @Test
        @DisplayName("성공 - imageKeys에 null/blank가 포함돼도 필터링 후 저장한다")
        void 기록_생성_성공_이미지키_필터링() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            List<String> imageKeys = Arrays.asList("record/users/1/a.png", null, "  ");
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, imageKeys);

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(libraryRepository.findByIdAndUserIdForUpdate(20L, 1L)).willReturn(Optional.of(library));
            given(recordRepository.countByLibraryIdAndUserId(20L, 1L)).willReturn(0L);

            // when
            recordService.createRecord(user, 10L, request);

            // then
            verify(recordRepository).save(any(Record.class));
            verify(recordImageRepository).save(any(RecordImage.class));
            verify(timelineCommandService).appendRecordCreated(any(Record.class), eq(1));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 책이면 예외를 던진다")
        void 기록_생성_실패_책없음() {
            // given
            User user = UserFixture.user();
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, List.of());

            given(bookRepository.findById(10L)).willReturn(Optional.empty());

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.createRecord(user, 10L, request));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 서재에 등록되지 않은 책이면 예외를 던진다")
        void 기록_생성_실패_서재미등록() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, List.of());

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(null);

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.createRecord(user, 10L, request));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("실패 - 비관적 락 획득 실패 시 예외를 던진다")
        void 기록_생성_실패_락획득실패() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, List.of());

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(libraryRepository.findByIdAndUserIdForUpdate(20L, 1L)).willReturn(Optional.empty());

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.createRecord(user, 10L, request));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }

        @Test
        @DisplayName("실패 - 기록 개수가 최대를 초과하면 예외를 던진다")
        void 기록_생성_실패_최대개수초과() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            RecordRequestDto request = new RecordRequestDto("내용", Emotion.FUN, List.of());

            given(bookRepository.findById(10L)).willReturn(Optional.of(book));
            given(libraryRepository.findByUserAndBook(user, book)).willReturn(library);
            given(libraryRepository.findByIdAndUserIdForUpdate(20L, 1L)).willReturn(Optional.of(library));
            given(recordRepository.countByLibraryIdAndUserId(20L, 1L)).willReturn(1000L);

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.createRecord(user, 10L, request));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(FileErrorCode.FILE_NUM_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("기록 삭제")
    class DeleteRecord {

        @Test
        @DisplayName("성공 시 이미지 정리 이벤트를 발행한다")
        void 기록_삭제_성공() {
            // given
            User user = UserFixture.user();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(user, book);
            Record record = record(library, Emotion.USEFUL, "유용한 개발 책이었다.");
            record.getImages().add(new RecordImage(record, "record/users/1/test.png", 0));
            ArgumentCaptor<RecordDeletedEvent> captor = ArgumentCaptor.forClass(RecordDeletedEvent.class);

            given(recordRepository.findById(1L)).willReturn(Optional.of(record));

            // when
            recordService.deleteRecord(user, 1L);

            // then
            verify(recordRepository).delete(record);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().recordId()).isEqualTo(1L);
            assertThat(captor.getValue().imageKeys()).containsExactly("record/users/1/test.png");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록이면 예외를 던진다")
        void 기록_삭제_실패_기록없음() {
            // given
            User user = UserFixture.user();
            given(recordRepository.findById(1L)).willReturn(Optional.empty());

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.deleteRecord(user, 1L));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 예외를 던진다")
        void 기록_삭제_실패_권한없음() {
            // given
            User owner = UserFixture.user();
            User other = UserFixture.anotherUser();
            Book book = BookFixture.book();
            Library library = LibraryFixture.library(owner, book);
            Record record = record(library, null, "기록");

            given(recordRepository.findById(1L)).willReturn(Optional.of(record));

            // when
            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.deleteRecord(other, 1L));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_AUTHORIZED);
        }
    }

    @Nested
    @DisplayName("기록 수정")
    class UpdateRecord {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("성공 - 기존 이미지가 있을 때 새 이미지로 수정한다")
            void 기록_수정_성공_이미지교체_내용수정() {
                // given
                User user = UserFixture.user();
                Book book = BookFixture.book();
                Library library = LibraryFixture.library(user, book);
                Record record = record(library, Emotion.FUN, "재미있는 책이었다.");

                // 기존 이미지 있다고 가정
                RecordImage existingImage = recordImage(record, "record/users/1/old.png", 0);
                record.getImages().add(existingImage);

                // 수정 요청 Request DTO
                RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                        "유용한 책이었다.", Emotion.USEFUL, List.of("record/users/1/updated.png"));

                given(recordRepository.findById(1L)).willReturn(Optional.of(record));

                // when
                recordService.updateRecord(user, 1L, request);

                // then
                assertThat(record.getContent()).isEqualTo("유용한 책이었다.");
                assertThat(record.getEmotion()).isEqualTo(Emotion.USEFUL);

                // 기존 이미지 삭제 검증
                verify(recordImageRepository).delete(existingImage);
                // 새 이미지가 저장되었는지 검증
                verify(recordImageRepository).save(any(RecordImage.class));
                assertThat(record.getImages()).isEmpty();
                verify(eventPublisher).publishEvent(any(RecordDeletedEvent.class));
            }

            @Test
            @DisplayName("성공 - 이미지 없이 내용과 감정이 수정된다")
            void 기록_수정_성공_내용수정_이미지없음() {
                // given
                User user = UserFixture.user();
                Book book = BookFixture.book();
                Library library = LibraryFixture.library(user, book);
                Record record = record(library, Emotion.FUN, "재미있는 책이었다.");

                // 수정 요청 Request DTO
                RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                        "유용한 책이었다.", Emotion.USEFUL, List.of());

                given(recordRepository.findById(1L)).willReturn(Optional.of(record));

                // when
                recordService.updateRecord(user, 1L, request);

                // then
                assertThat(record.getContent()).isEqualTo("유용한 책이었다.");
                assertThat(record.getEmotion()).isEqualTo(Emotion.USEFUL);
                assertThat(record.getImages()).isEmpty();

                // 이미지 저장/삭제가 호출되지 않아야 함
                verify(recordImageRepository, never()).save(any(RecordImage.class));
                verify(recordImageRepository, never()).delete(any(RecordImage.class));
            }

            @Test
            @DisplayName("성공 - 타임라인 생성을 호출하지 않는다")
            void 기록_수정_성공_타임라인호출없음() {
                // given
                User user = UserFixture.user();
                Book book = BookFixture.book();
                Library library = LibraryFixture.library(user, book);
                Record record = record(library, Emotion.FUN, "재미있는 책이었다.");

                // 수정 요청 Request DTO
                RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                        "유용한 책이었다.", Emotion.USEFUL, List.of());

                given(recordRepository.findById(1L)).willReturn(Optional.of(record));

                // when
                recordService.updateRecord(user, 1L, request);

                // 타임라인에는 수정이므로 새 이미지 개수는 0으로 전달
                verify(timelineCommandService, never()).appendRecordCreated(any(), anyInt());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("실패 - 존재하지 않는 기록이면 예외를 던진다")
            void 기록_수정_실패_기록없음() {
                // given
                User user = UserFixture.user();
                RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                        "수정 내용", Emotion.USEFUL, List.of());

                given(recordRepository.findById(1L)).willReturn(Optional.empty());

                // when
                CustomException ex = assertThrows(CustomException.class,
                        () -> recordService.updateRecord(user, 1L, request));

                // then
                assertThat(ex.getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND);
            }

            @Test
            @DisplayName("실패 - 권한이 없으면 예외를 던진다")
            void 기록_수정_실패_권한없음() {
                // given
                User owner = UserFixture.user();
                User other = UserFixture.anotherUser();
                Book book = BookFixture.book();
                Library library = LibraryFixture.library(owner, book);
                Record record = record(library, Emotion.FUN, "기록");
                RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                        "수정 내용", Emotion.USEFUL, List.of());

                given(recordRepository.findById(1L)).willReturn(Optional.of(record));

                // when
                CustomException ex = assertThrows(CustomException.class,
                        () -> recordService.updateRecord(other, 1L, request));

                // then
                assertThat(ex.getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_AUTHORIZED);
            }
        }
    }

    private Record record(Library library, Emotion emotion, String content) {
        Record record = Record.create(library, emotion, content);
        ReflectionTestUtils.setField(record, "id", 1L);
        return record;
    }

    private RecordImage recordImage(Record record, String imageKey, int order) {
        RecordImage image = new RecordImage(record, imageKey, order);
        ReflectionTestUtils.setField(image, "id", 100L);
        return image;
    }
}
