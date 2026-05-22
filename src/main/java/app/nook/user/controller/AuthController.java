package app.nook.user.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.oauth.OAuthService;
import app.nook.user.service.UserService;
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

}
