package app.nook.user.event;

import java.util.List;

/**
 * 회원 hard delete 시 S3 에서 삭제해야 할 이미지 key 목록.
 * (프로필 이미지 + 유저의 모든 기록 이미지)
 * DB 트랜잭션 커밋 후 실제 S3 삭제를 수행하기 위한 이벤트.
 */
public record UserImageCleanupEvent(Long userId, List<String> imageKeys) {
}
