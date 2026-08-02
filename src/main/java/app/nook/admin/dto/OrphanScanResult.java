package app.nook.admin.dto;

import java.util.List;

/**
 * 고아 이미지 대조/삭제 결과.
 *
 * @param scanned     스캔한 R2 객체 수(관리 prefix 한정)
 * @param orphanCount 고아로 판정된 수 (삭제 모드에서는 실제 삭제 성공 수)
 * @param orphanKeys  고아 key 목록 (dry-run 에서만 채워짐)
 * @param deleted     실제 삭제를 수행했는지 여부 (false=dry-run)
 */
public record OrphanScanResult(
        int scanned,
        int orphanCount,
        List<String> orphanKeys,
        boolean deleted
) {}
