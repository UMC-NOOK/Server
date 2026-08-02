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
 * 회원 완전 삭제(hard delete) 스케줄러.
 * <p>
 * soft delete({@link WithdrawService#softDelete}) 후 유예기간(기본 14일)이 지난 계정을
 * 주기적으로 찾아 완전 삭제한다. 유예기간 안에 동일 소셜로 재로그인하면 계정이 복구되므로,
 * 그 기간이 지난 계정만 실제로 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserRepository userRepository;
    private final WithdrawService withdrawService;

    /** 탈퇴 유예기간(일). 이 기간이 지난 soft delete 계정을 완전 삭제함 */
    @Value("${withdraw.grace-period-days:14}")
    private long gracePeriodDays;

    /**
     * 매일 새벽 4시에 유예기간이 지난 탈퇴 계정을 완전 삭제한다.
     * 계정별로 독립 트랜잭션(WithdrawService.hardDelete)에서 처리하여
     * 한 건 실패가 나머지에 영향을 주지 않도록 한다.
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
