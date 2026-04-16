package app.nook.user.event;

import app.nook.r2.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileImageCleanupListener {

    private final PresignedUrlService presignedUrlService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProfileImageCleanupEvent event) {
        try {
            log.info("[PROFILE_IMAGE_CLEANUP] profileImageKey={}", event.profileImageKey());
            presignedUrlService.deleteFile(event.profileImageKey());
        } catch (RuntimeException e) {
            log.warn("[PROFILE_IMAGE_CLEANUP_FAILED] profileImageKey={}", event.profileImageKey(), e);
        }
    }
}
