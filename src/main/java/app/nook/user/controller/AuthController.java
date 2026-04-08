package app.nook.user.controller;

import app.nook.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.oauth.OAuthService;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final OAuthService oAuthService;
    private final UserService userService;

    /**
     * OAuth 로그인 (Google / Kakao)
     * 프론트에서 code 전달
     *
     * POST /auth/oauth
     */
    @PostMapping("/oauth")
    public ApiResponse<UserDTO.LoginResponse> oauthLogin(
            @Valid @RequestBody OAuthDTO.OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        return ApiResponse.onSuccess(oAuthService.login(
                request.getProvider(),
                request.getCode(),
                response
        ), SuccessCode.OK);
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

    // 현재 로그인한 유저 확인
    @GetMapping("/me")
    public ApiResponse<UserDTO.LoginResponse> user(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        return ApiResponse.onSuccess(userService.getThisUser(userDetails.getUser()),SuccessCode.OK);
    }
}
