package app.nook.user.event;

import app.nook.r2.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 hard delete 커밋 후, 유저가 가지고 있던 이미지들을 S3 에서 삭제한다.
 * DB 삭제가 성공적으로 커밋된 뒤에만 실행되어, 롤백 시 S3 파일이 유실되는 것을 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserImageCleanupListener {

    private final PresignedUrlService presignedUrlService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserImageCleanupEvent event) {
        for (String key : event.imageKeys()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                presignedUrlService.deleteFile(key);
            } catch (Exception e) {
                log.warn("[USER_IMAGE_CLEANUP] S3 이미지 삭제 실패 userId={}, key={}", event.userId(), key, e);
            }
        }
        log.info("[USER_IMAGE_CLEANUP] 완료 userId={}, count={}", event.userId(), event.imageKeys().size());
    }
}
