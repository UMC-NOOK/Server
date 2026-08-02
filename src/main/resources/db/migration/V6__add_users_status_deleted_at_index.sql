-- UserRepository.findByStatusAndDeletedAtBefore 조회용 복합 인덱스
-- (탈퇴 유예기간 경과 계정을 스케줄러가 완전 삭제 시 사용)

CREATE INDEX IF NOT EXISTS idx_users_status_deleted_at
    ON users (status, deleted_at);
