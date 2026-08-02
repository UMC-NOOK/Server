package app.nook.admin.controller;

import app.nook.admin.AdminAccessChecker;
import app.nook.admin.dto.OrphanScanResult;
import app.nook.admin.service.OrphanImageService;
import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 이미지 관리 API
 * URL 단 hasRole("ADMIN") + 컨트롤러 이메일 확인(AdminAccessChecker) 이중 게이트
 */
@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/admin/images")
public class AdminImageController {

    private final AdminAccessChecker adminAccessChecker;
    private final OrphanImageService orphanImageService;

    /**
     * 고아 이미지 대조 (dry-run) — 삭제하지 않고 목록만 반환
     * GET /admin/images/orphans
     */
    @GetMapping("/orphans")
    public ApiResponse<OrphanScanResult> scanOrphans(@CurrentUser User user) {
        adminAccessChecker.verifyAdmin(user);
        return ApiResponse.onSuccess(orphanImageService.scan(), SuccessCode.OK);
    }

    /**
     * 고아 이미지 삭제 — 24시간 지난 미참조 이미지 실제 삭제
     * DELETE /admin/images/orphans
     */
    @DeleteMapping("/orphans")
    public ApiResponse<OrphanScanResult> deleteOrphans(@CurrentUser User user) {
        adminAccessChecker.verifyAdmin(user);
        return ApiResponse.onSuccess(orphanImageService.deleteOrphans(), SuccessCode.OK);
    }
}
