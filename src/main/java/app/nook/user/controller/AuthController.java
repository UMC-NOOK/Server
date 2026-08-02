package app.nook.user.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.BearerTokenResolver;
import app.nook.user.oauth.OAuthService;
import app.nook.user.service.UserService;
import app.nook.user.service.WithdrawService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final OAuthService oAuthService;
    private final UserService userService;
    private final WithdrawService withdrawService;

    /**
     * OAuth 로그인 (Google / Kakao)
     * 프론트에서 code 전달
     *
     * POST /auth/oauth
     */
    @PostMapping("/oauth")
    public ApiResponse<UserDTO.LoginResponse> oauthLogin(
            @Valid @RequestBody OAuthDTO.OAuthLoginRequest request
    ) {
        return ApiResponse.onSuccess(oAuthService.login(
                request.getProvider(),
                request.getCode()
        ), SuccessCode.OK);
    }

    /**
     * 탈퇴 유예중 계정 복구 후 로그인
     * - 로그인 응답의 recoveryRequired=true 와 recoveryToken(1시간)을 받고,
     *   사용자가 복구를 확정하면 그 토큰으로 호출한다. (소셜 재인증 불필요)
     * POST /auth/recover
     */
    @PostMapping("/recover")
    public ApiResponse<UserDTO.LoginResponse> recover(
            @Valid @RequestBody UserDTO.RecoveryRequest request
    ) {
        return ApiResponse.onSuccess(
                userService.recover(request.recoveryToken()),
                SuccessCode.OK
        );
    }

    /**
     * DEV 로그인
     * - 회원가입 x
     * - 기존 유저만 허용
     *             email:    "dev@test.com",
     *             nickname:     "DEV_USER"
     * POST /auth/dev/login
     */
    @PostMapping("/dev/login")
    public ApiResponse<UserDTO.LoginResponse> devLogin(
            @RequestBody UserDTO.DevLoginRequest request
    ) {
        return ApiResponse.onSuccess(
                userService.devLogin(request.getEmail()),
                SuccessCode.OK
        );
    }

    /**
     * DEV 회원가입
     * - email 기준 신규 생성
     * - USER 권한으로 생성 후 로그인 토큰 발급
     */
    @PostMapping("/dev/signup")
    public ApiResponse<UserDTO.LoginResponse> devSignUp(
            @RequestBody UserDTO.DevSignUpRequest request
    ) {
        return ApiResponse.onSuccess(
                userService.devSignUp(request),
                SuccessCode.CREATED
        );
    }

    @PostMapping("/reissue")
    public ApiResponse<UserDTO.TokenReissueResponse> reissueAccessToken(
            @Valid @RequestBody UserDTO.TokenReissueRequest request
    ) {
        return ApiResponse.onSuccess(
                userService.reissueAccessToken(request.getRefreshToken()),
                SuccessCode.OK
        );
    }

    // 현재 로그인한 유저 확인
    @GetMapping("/me")
    public ApiResponse<UserDTO.UserInfo> user(
            @CurrentUser User user
            ){
        return ApiResponse.onSuccess(userService.getThisUser(user),SuccessCode.OK);
    }

    /**
     * 로그아웃
     * - refresh 제거 + 현재 access token 블랙리스트
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CurrentUser User user,
            HttpServletRequest request
    ) {
        userService.logout(user, BearerTokenResolver.resolve(request));
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    /**
     * 회원탈퇴 (soft delete) — 복구 가능
     * - status=DELETED, 토큰 무효화 (데이터/사진/소셜 연결 보존, unlink 안 함)
     * - 동일 소셜로 재로그인하면 자동 복구
     * - 유예기간 경과 후 UserScheduler 가 완전 삭제(hard delete) 처리
     * DELETE /auth/withdraw
     */
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(
            @CurrentUser User user,
            HttpServletRequest request
    ) {
        withdrawService.softDelete(user, BearerTokenResolver.resolve(request));
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

}
