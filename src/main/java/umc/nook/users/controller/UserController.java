package umc.nook.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.lounge.dto.LoungeResponseDTO;
import umc.nook.lounge.service.LoungeService;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.service.CustomUserDetails;
import umc.nook.users.service.UserService;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "회원 API")
public class UserController {

    private final UserService userService;
    private final LoungeService loungeService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행합니다.")
    public ApiResponse<UserDTO.UserResponseDTO> signup(@RequestBody @Validated UserDTO.SignUpDto request) {
        return ApiResponse.onSuccess(userService.signup(request), SuccessCode.OK);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인 후 토큰을 발급합니다.")
    public ApiResponse<UserDTO.LoginResponseDTO> login(@RequestBody @Validated UserDTO.LoginDto request, HttpServletResponse response) {
        UserDTO.LoginResponseDTO responseWithToken = userService.login(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", responseWithToken.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.setHeader("Set-Cookie", refreshTokenCookie.toString());

        return ApiResponse.onSuccess(responseWithToken, SuccessCode.OK);
    }


    @PostMapping("/reissue")
    @Operation(summary = "엑세스 토큰 재발급")
    public ApiResponse<UserDTO.TokenResponseDto> recreateAccessToken(HttpServletRequest request, HttpServletResponse response) {
        UserDTO.TokenResponseDto responseDto = userService.reissue(request);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", responseDto.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        response.setHeader("Set-Cookie", refreshTokenCookie.toString());
        return ApiResponse.onSuccess(responseDto, SuccessCode.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 처리합니다.")
    public ApiResponse<String> logout(HttpServletRequest request) {
        String refreshToken = request.getHeader("X-Refresh-Token");
        userService.logout(refreshToken);
        return ApiResponse.onSuccess("로그아웃 되었습니다.",SuccessCode.OK);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 유저의 정보를 조회합니다.")
    public ApiResponse<UserDTO.UserResponseDTO> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(userService.getUserInfo(userDetails.getUser()),SuccessCode.OK);
    }

    @PostMapping("/kakao/login")
    @Operation(summary = "카카오 로그인 (인가 코드 기반)",
            description = "인가 코드 발급 url을 통해 받은 인가 코드를 입력하여 카카오 로그인을 진행합니다.")
    public ApiResponse<UserDTO.KakaoLoginResponseDTO> kakaoLogin(@RequestParam("code") String code, HttpServletResponse response) {

        UserDTO.KakaoLoginResponseDTO kakaoResponse = userService.kakaoLogin(code);

        // 1. JWT 리프레시 토큰 쿠키
        ResponseCookie jwtRefreshTokenCookie = ResponseCookie.from("refreshToken", kakaoResponse.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        // 2. 카카오 리프레시 토큰 쿠키
        ResponseCookie kakaoRefreshTokenCookie = ResponseCookie.from("kakaoRefreshToken", kakaoResponse.getKakaoRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(60))
                .build();

        response.setHeader("Set-Cookie", jwtRefreshTokenCookie.toString());
        response.addHeader("Set-Cookie", kakaoRefreshTokenCookie.toString());

        return ApiResponse.onSuccess(kakaoResponse, SuccessCode.OK);
    }


    @PostMapping("/kakao/reissue")
    @Operation(summary = "카카오 엑세스 토큰 재발급")
    public ApiResponse<UserDTO.TokenResponseDto> kakaoReissue(
            HttpServletRequest request,
            HttpServletResponse response) {

        UserDTO.TokenResponseDto responseDto = userService.kakaoReissue(request);
        // 쿠키 갱신
        ResponseCookie refreshTokenCookie = ResponseCookie.from("kakaoRefreshToken", responseDto.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        response.setHeader("Set-Cookie", refreshTokenCookie.toString());

        return ApiResponse.onSuccess(responseDto, SuccessCode.OK);
    }

    @PostMapping("/kakao/logout")
    @Operation(summary = "카카오 로그아웃")
    public ApiResponse<String> kakaoLogout(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) {
        userService.kakaoLogout(userDetails.getUser());

        // 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.setHeader("Set-Cookie", deleteCookie.toString());

        return ApiResponse.onSuccess("로그아웃 되었습니다." , SuccessCode.OK);
    }



    @Operation(
            summary = "홈 화면 선호 카테고리",
            description = """
            홈 화면에서 가장 많이 읽은 카테고리들을 조회합니다.
            top 5 + 나머지는 기타로 합쳐서 처리합니다.
            """
    )
    @GetMapping("/categories")
    public ApiResponse<LoungeResponseDTO.CategoryResultDTO> getFavoriteCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(loungeService.getFavoriteCategories(userDetails), SuccessCode.OK);
    }

    @Operation(
            summary = "홈 화면 목표 조회",
            description = """
            홈 화면에서 설정한 독서 목표를 조회합니다.
            """
    )
    @GetMapping("/goals")
    public ApiResponse<LoungeResponseDTO.GoalResultDTO> getGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(loungeService.getGoal(userDetails), SuccessCode.OK);
    }

    @Operation(
            summary = "홈 화면 목표 설정",
            description = """
            홈 화면에서 독서 목표를 설정합니다.
            목표는 50, 100, 150, 200, 250, 300 중 하나의 값입니다.
            """
    )
    @PatchMapping("/goals")
    public ApiResponse<Void> modifyGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody LoungeResponseDTO.GoalRequestDTO goalRequestDTO
    ) {
        loungeService.modifyGoal(userDetails, goalRequestDTO);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴",
            description = "로그인한 본인 계정을 탈퇴 처리합니다. 계정 정보는 30일 후에 자동 삭제됩니다.")
    public ApiResponse<String> withdrawMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String msg = userService.withdrawUser(userDetails.getUser());
        return ApiResponse.onSuccess(msg,SuccessCode.OK);
    }
}
