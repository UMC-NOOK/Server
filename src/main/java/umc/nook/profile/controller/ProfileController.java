package umc.nook.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.profile.domain.Profile;
import umc.nook.profile.dto.ProfileRequestDTO;
import umc.nook.profile.service.ProfileService;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profiles")
@Tag(name = "Profile API", description = "프로필 API")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "프로필을 수정합니다.")
    @PatchMapping
    public ApiResponse<Long> updateProfile(@RequestBody ProfileRequestDTO dto,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        Long userId = profileService.updateProfile(dto, user);
        return ApiResponse.onSuccess(userId, SuccessCode.OK);
    }
}
