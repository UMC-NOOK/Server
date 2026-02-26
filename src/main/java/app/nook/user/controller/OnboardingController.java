package app.nook.user.controller;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OnboardingDto.CompleteResponse> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute OnboardingDto.CompleteRequest request
    ) {
        return ApiResponse.onSuccess(onboardingService.completeOnboarding(userDetails.getUser().getId(), request), SuccessCode.OK);
    }

    @GetMapping("/status")
    public ApiResponse<OnboardingDto.StatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                onboardingService.getOnboardingStatus(userDetails.getUser().getId()),
                SuccessCode.OK
        );
    }

    @GetMapping("/goal")
    public ApiResponse<OnboardingDto.GoalResponse> getGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(onboardingService.getGoal(userDetails.getUser().getId()), SuccessCode.OK);
    }

    @PatchMapping("/goal")
    public ApiResponse<OnboardingDto.GoalUpdateResponse> updateGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingDto.GoalUpdateRequest request
    ) {
        return ApiResponse.onSuccess(
                onboardingService.updateGoal(userDetails.getUser().getId(), request),
                SuccessCode.OK
        );
    }
}
