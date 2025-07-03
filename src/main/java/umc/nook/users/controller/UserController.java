package umc.nook.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
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

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행합니다.")
    public ApiResponse<UserDTO.UserResponseDTO> signup(@RequestBody UserDTO.SignUpDto request) {
        return ApiResponse.onSuccess(userService.signup(request), SuccessCode.OK);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인 후 토큰을 발급합니다.")
    public ApiResponse<UserDTO.LoginResponseDTO> login(@RequestBody UserDTO.LoginDto request, HttpServletResponse response) {
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
}
