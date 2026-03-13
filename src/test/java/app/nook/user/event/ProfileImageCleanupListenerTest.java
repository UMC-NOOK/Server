package app.nook.user.event;

import app.nook.book.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileImageCleanupListenerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProfileImageCleanupListener listener;

    @Test
    @DisplayName("프로필 이미지 정리 이벤트를 받으면 기존 프로필 이미지를 삭제한다")
    void handle_deleteOldProfileImage() {
        ProfileImageCleanupEvent event = new ProfileImageCleanupEvent("/uploads/profiles/old.png");

        listener.handle(event);

        verify(fileStorageService).deleteProfileByUrl("/uploads/profiles/old.png");
    }
}
