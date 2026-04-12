package app.nook.record.service;

import app.nook.book.domain.Book;
import app.nook.book.repository.BookRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.event.RecordDeletedEvent;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.record.repository.RecordRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.assertj.core.api.Assertions;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordService 테스트")
public class RecordServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordImageRepository recordImageRepository;

    @InjectMocks
    private RecordService recordService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user() {
        return User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
    }

    @Nested
    @DisplayName("기록 등록")
    class Save {



    }

    @Nested
    @DisplayName("기록 삭제")
    class DeleteRecord {

        @Test
        @DisplayName("성공 시 이미지 정리 이벤트를 발행한다")
        void 기록_삭제_성공() {
            User user = user();
            ReflectionTestUtils.setField(user, "id", 1L);

            Book book = Book.builder().title("책").build();
            Library library = Library.builder().user(user).book(book).build();
            Record record = Record.builder()
                    .library(library)
                    .content("기록")
                    .build();
            record.getImages().add(new RecordImage(record, "record/users/1/test.png", 0));

            given(recordRepository.findById(1L)).willReturn(Optional.of(record));

            recordService.deleteRecord(user, 1L);

            ArgumentCaptor<RecordDeletedEvent> captor = ArgumentCaptor.forClass(RecordDeletedEvent.class);
            verify(recordRepository).delete(record);
            verify(eventPublisher).publishEvent(captor.capture());
            Assertions.assertThat(captor.getValue().recordId()).isEqualTo(1L);
            Assertions.assertThat(captor.getValue().imageKeys()).containsExactly("record/users/1/test.png");
        }

        @Test
        @DisplayName("권한이 없으면 예외를 던진다")
        void 기록_삭제_실패_권한없음() {
            User owner = user();
            ReflectionTestUtils.setField(owner, "id", 1L);
            User other = user();
            ReflectionTestUtils.setField(other, "id", 2L);

            Book book = Book.builder().title("책").build();
            Library library = Library.builder().user(owner).book(book).build();
            Record record = Record.builder()
                    .library(library)
                    .content("기록")
                    .build();

            given(recordRepository.findById(1L)).willReturn(Optional.of(record));

            CustomException ex = assertThrows(CustomException.class,
                    () -> recordService.deleteRecord(other, 1L));
            Assertions.assertThat(ex.getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_AUTHORIZED);
        }
    }
}
