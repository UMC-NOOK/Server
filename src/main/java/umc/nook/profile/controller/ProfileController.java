package umc.nook.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.profile.domain.BackgroundPattern;
import umc.nook.profile.domain.CharacterColor;
import umc.nook.profile.dto.ProfileResponseDTO;
import umc.nook.profile.service.ProfileService;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profiles")
@Tag(name = "Profile API", description = "프로필 API")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "프로필을 수정합니다.")
    @PatchMapping
    public ApiResponse<Long> updateProfile(
            @Parameter(description = "사용자 별명", example = "프로 독자")
            @RequestParam String alias,

            @Parameter(
                    description = "캐릭터 색상",
                    example = "ORANGE"
            )
            @RequestParam CharacterColor characterColor,

            @Parameter(
                    description = "배경 패턴",
                    example = "NONE"
            )
            @RequestParam BackgroundPattern backgroundPattern,

            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = profileService.updateProfile(alias, characterColor, backgroundPattern, user);
        return ApiResponse.onSuccess(userId, SuccessCode.OK);
    }

    @Operation(summary = "프로필 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<ProfileResponseDTO> getProfile(@AuthenticationPrincipal CustomUserDetails user) {
        ProfileResponseDTO response = profileService.getProfile(user);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(summary = "사용자 이름을 수정합니다.")
    @PutMapping("/nicknames")
    public ApiResponse<Long> updateNickname(@RequestParam String nickname, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = profileService.updateNickname(nickname, userDetails);
        return ApiResponse.onSuccess(userId, SuccessCode.OK);
    }
}
