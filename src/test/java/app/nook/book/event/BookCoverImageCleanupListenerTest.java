package app.nook.book.event;

import app.nook.r2.service.PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookCoverImageCleanupListenerTest {

    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private BookCoverImageCleanupListener listener;

    @Test
    @DisplayName("책 표지 이미지 정리 이벤트를 받으면 기존 표지 이미지를 삭제한다")
    void handle_deleteOldCoverImage() {
        BookCoverImageCleanupEvent event = new BookCoverImageCleanupEvent("book/users/1/old.png");

        listener.handle(event);

        verify(presignedUrlService).deleteFile("book/users/1/old.png");
    }

    @Test
    @DisplayName("책 표지 이미지 삭제 실패 시 예외를 던지지 않는다")
    void handle_deleteFailure_noException() {
        BookCoverImageCleanupEvent event = new BookCoverImageCleanupEvent("book/users/1/missing.png");
        doThrow(new RuntimeException("delete failed"))
                .when(presignedUrlService)
                .deleteFile("book/users/1/missing.png");

        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
    }
}
