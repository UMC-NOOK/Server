package app.nook.user.scheduler;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserStatus;
import app.nook.user.repository.UserRepository;
import app.nook.user.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 완전 삭제(hard delete) 스케줄러
 * <p>
 * soft delete({@link WithdrawService#softDelete}) 후 유예기간(기본 14일) 경과 계정을
 * 주기적으로 탐색하여 완전 삭제 — 유예기간 내 동일 소셜 재로그인 시 계정 복구되므로
 * 유예기간 경과 계정만 실제 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserRepository userRepository;
    private final WithdrawService withdrawService;

    /** 탈퇴 유예기간(일) — 경과한 soft delete 계정 완전 삭제 대상 */
    @Value("${withdraw.grace-period-days:14}")
    private long gracePeriodDays;

    /**
     * 매일 새벽 4시 유예기간 경과 탈퇴 계정 완전 삭제
     * 계정별 독립 트랜잭션(WithdrawService.hardDelete) 처리 →
     * 한 건 실패가 나머지에 미영향
     */
    @Scheduled(cron = "${withdraw.hard-delete-cron:0 0 4 * * *}")
    public void hardDeleteExpiredWithdrawnUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(gracePeriodDays);
        List<User> targets =
                userRepository.findByStatusAndDeletedAtBefore(UserStatus.DELETED, threshold);

        if (targets.isEmpty()) {
            return;
        }

        log.info("[USER_SCHEDULER] 완전 삭제 대상 {}건 (gracePeriodDays={})", targets.size(), gracePeriodDays);
        int success = 0;
        for (User user : targets) {
            try {
                withdrawService.hardDelete(user);
                success++;
            } catch (Exception e) {
                log.error("[USER_SCHEDULER] hard delete 실패 userId={}", user.getId(), e);
            }
        }
        log.info("[USER_SCHEDULER] 완전 삭제 완료 {}/{}건", success, targets.size());
    }
}
