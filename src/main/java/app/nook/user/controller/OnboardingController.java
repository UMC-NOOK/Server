package app.nook.user.controller;

import app.nook.book.service.FileStorageService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.OnboardingService;
import app.nook.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/onboarding")
public class OnboardingController {

    private final FileStorageService fileStorageService;
    private final OnboardingService onboardingService;

    @PostMapping("/complete")
    public ApiResponse<OnboardingDto.CompleteResponse> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingDto.CompleteRequest request
    ) {
        return ApiResponse.onSuccess(onboardingService.completeOnboarding(userDetails.getUser(), request), SuccessCode.OK);
    }

    @GetMapping("/status")
    public ApiResponse<OnboardingDto.StatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                onboardingService.getOnboardingStatus(userDetails.getUser()),
                SuccessCode.OK
        );
    }

    @PatchMapping("/goal")
    public ApiResponse<OnboardingDto.GoalUpdateResponse> updateGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingDto.GoalUpdateRequest request
    ) {
        return ApiResponse.onSuccess(
                onboardingService.updateGoal(userDetails.getUser(), request),
                SuccessCode.OK
        );
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OnboardingDto.ProfileImageUploadResponse> uploadProfileImage(
            @RequestPart("profileImage") MultipartFile profileImage
            ) {
        String profileUrl = fileStorageService.uploadProfile(profileImage);
        return ApiResponse.onSuccess(new OnboardingDto.ProfileImageUploadResponse(profileUrl), SuccessCode.OK);
    }
}
