package app.nook.record.event;

import app.nook.r2.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordDeletedEventListener {

    private final PresignedUrlService presignedUrlService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(RecordDeletedEvent event) {
        for (String imageKey : event.imageKeys()) {
            try {
                presignedUrlService.deleteFile(imageKey);
            } catch (Exception e) {
                log.warn("[RECORD] R2 스토리지에서 기록 이미지 삭제 실패 recordId={}, key={}", event.recordId(), imageKey, e);
            }
        }
    }
}
