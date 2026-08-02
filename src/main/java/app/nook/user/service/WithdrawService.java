package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.record.repository.RecordImageRepository;
import app.nook.user.domain.User;
import app.nook.user.event.UserImageCleanupEvent;
import app.nook.user.oauth.OAuthService;
import app.nook.user.redis.TokenBlacklistService;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 회원 탈퇴 / 복구 전담 서비스
 * <p>
 * - {@link #softDelete} : 복구 가능한 탈퇴 (데이터/사진/소셜 연결 보존, 소셜 unlink 안 함)
 * - {@link #hardDelete} : 완전 탈퇴 (소셜 unlink + 연관 데이터 전부 삭제 + S3 이미지 삭제)
 * - {@link #recoverUser} : soft delete 된 계정 복구
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final RecordImageRepository recordImageRepository;
    private final TokenRedisRepository tokenRedisRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final OAuthService oAuthService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원탈퇴 (soft delete)
     * - status=DELETED, deletedAt 기록 (데이터/사진/소셜 연결 모두 보존 → 재로그인 시 복구 가능)
     * - 소셜 unlink 는 하지 않음 (재로그인 시 복구 가능)
     * - refresh 제거 + access 블랙리스트
     */
    @Transactional
    public void softDelete(User user, String accessToken) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        managed.withdraw();

        invalidateTokens(managed.getId(), accessToken);
        log.info("[WITHDRAW-SOFT] userId={}", managed.getId());
    }

    /**
     * 완전 탈퇴 (hard delete) — {@link app.nook.user.scheduler.UserScheduler} 가 유예기간 경과 계정에 대해 호출
     * soft delete 시점에 토큰 무효화 + access token 만료 완료 상태라 별도 토큰 처리는 방어적 수준만 수행
     * 1. 소셜 연결 해제(카카오 unlink 등)
     * 2. 삭제 전 S3 이미지 key 수집 (프로필 + 모든 기록 이미지)
     * 3. user 삭제 → DB ON DELETE CASCADE 로 library/record/focus/timeline/검색·조회 기록 전부 삭제
     * 4. 커밋 후 S3 실물 이미지 삭제 (AFTER_COMMIT 리스너)
     */
    @Transactional
    public void hardDelete(User user) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        Long userId = managed.getId();

        // 1. 소셜 연결 해제 (완전 탈퇴에서만)
        oAuthService.unlinkUser(managed);

        // 2. 삭제되기 전에 S3 이미지 key 수집
        List<String> imageKeys = collectImageKeys(managed);

        // 3. 잔여 refresh token 방어적 제거 후 DB 삭제 (연관 데이터는 FK ON DELETE CASCADE 로 함께 삭제)
        tokenRedisRepository.deleteById(userId);
        userRepository.delete(managed);

        // 4. 커밋 후 S3 실물 이미지 삭제
        if (!imageKeys.isEmpty()) {
            eventPublisher.publishEvent(new UserImageCleanupEvent(userId, imageKeys));
        }
        log.info("[WITHDRAW-HARD] userId={}, imageCount={}", userId, imageKeys.size());
    }

    // 프로필 이미지 키 검색
    private List<String> collectImageKeys(User user) {
        List<String> keys = new ArrayList<>();
        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isBlank()) {
            keys.add(user.getProfileImageKey());
        }
        keys.addAll(recordImageRepository.findKeysByUserId(user.getId()));
        return keys;
    }

    // 토큰 무효화
    private void invalidateTokens(Long userId, String accessToken) {
        tokenRedisRepository.deleteById(userId);
        if (accessToken != null) {
            tokenBlacklistService.blacklist(accessToken);
        }
    }
}
