package app.nook.book.event;

import app.nook.r2.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookCoverImageCleanupListener {

    private final PresignedUrlService presignedUrlService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BookCoverImageCleanupEvent event) {
        try {
            log.info("[BOOK_COVER_IMAGE_CLEANUP] coverImageKey={}", event.coverImageKey());
            presignedUrlService.deleteFile(event.coverImageKey());
        } catch (RuntimeException e) {
            log.warn("[BOOK_COVER_IMAGE_CLEANUP_FAILED] coverImageKey={}", event.coverImageKey(), e);
        }
    }
}
