package app.nook.user.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import app.nook.user.dto.UserProfileDto;
import app.nook.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/users/me")
public class UserController {

    private final UserProfileService userProfileService;

    /**
     * 마이페이지 내 정보 조회
     * GET /users/me
     */
    @GetMapping
    public ApiResponse<UserProfileDto.MyPageResponse> getMyPage(
            @CurrentUser User user
    ) {
        return ApiResponse.onSuccess(userProfileService.getMyPage(user.getId()), SuccessCode.OK);
    }

    /**
     * 닉네임 수정
     * PATCH /users/me/nickname
     */
    @PatchMapping("/nickname")
    public ApiResponse<UserProfileDto.NickNameUpdateResponse> updateNickName(
            @CurrentUser User user,
            @Valid @RequestBody UserProfileDto.NickNameUpdateRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.updateNickName(user.getId(), request.nickName()), SuccessCode.OK);
    }

    /**
     * 프로필 이미지 수정
     * PATCH /users/me/profile-image
     */
    @PatchMapping("/profile-image")
    public ApiResponse<UserProfileDto.ProfileImageUpdateResponse> updateProfileImage(
            @CurrentUser User user,
            @Valid @RequestBody UserProfileDto.ProfileImageUpdateRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.updateProfileImage(user.getId(), request.profileImageKey()), SuccessCode.OK);
    }
}
