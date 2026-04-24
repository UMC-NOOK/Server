package app.nook.record.event;

import app.nook.r2.service.PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordDeletedEventListener 테스트")
class RecordDeletedEventListenerTest {

    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private RecordDeletedEventListener listener;

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("성공 - 삭제 대상 이미지 키를 모두 삭제한다")
        void 이미지키_전체삭제() {
            // given
            RecordDeletedEvent event = new RecordDeletedEvent(
                    1L,
                    List.of("record/users/1/a.png", "record/users/1/b.png")
            );

            // when
            listener.handle(event);

            // then
            verify(presignedUrlService).deleteFile("record/users/1/a.png");
            verify(presignedUrlService).deleteFile("record/users/1/b.png");
        }

        @Test
        @DisplayName("성공 - 일부 삭제가 실패해도 다음 이미지를 계속 삭제한다")
        void 일부삭제실패_다음이미지계속삭제() {
            // given
            RecordDeletedEvent event = new RecordDeletedEvent(
                    1L,
                    List.of("record/users/1/a.png", "record/users/1/b.png")
            );
            doThrow(new RuntimeException("delete failed"))
                    .when(presignedUrlService).deleteFile("record/users/1/a.png");

            // when
            listener.handle(event);

            // then
            verify(presignedUrlService).deleteFile("record/users/1/a.png");
            verify(presignedUrlService).deleteFile("record/users/1/b.png");
            verify(presignedUrlService, times(2)).deleteFile(org.mockito.ArgumentMatchers.anyString());
        }
    }
}
