package app.nook.user.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import app.nook.user.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEV/LOCAL 전용 인증 API.
 * prod 프로파일에서는 빈이 로드되지 않아 노출되지 않는다.
 */
@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/auth/dev")
@Profile({"local", "dev"})
public class DevAuthController {

    private final WithdrawService withdrawService;

    /**
     * 완전 탈퇴 (hard delete) — 테스트용 즉시 삭제
     * - 유예기간 없이 소셜 unlink + 연관 데이터/사진 전부 삭제
     * DELETE /auth/dev/withdraw
     */
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdrawPermanently(
            @CurrentUser User user
    ) {
        withdrawService.hardDelete(user);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }
}
