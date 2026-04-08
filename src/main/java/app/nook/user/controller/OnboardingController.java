package app.nook.user.controller;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    // 온보딩 최종 완료(프로필 이미지 key 포함)
    @PostMapping("/complete")
    public ApiResponse<OnboardingDto.CompleteResponse> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingDto.CompleteRequest request
    ) {
        return ApiResponse.onSuccess(onboardingService.completeOnboarding(userDetails.getUser().getId(), request), SuccessCode.OK);
    }

    // 온보딩 완료 여부 조회
    @GetMapping("/status")
    public ApiResponse<OnboardingDto.StatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                onboardingService.getOnboardingStatus(userDetails.getUser().getId()),
                SuccessCode.OK
        );
    }

    // 독서 목표/남은 권수 조회
    @GetMapping("/goal")
    public ApiResponse<OnboardingDto.GoalResponse> getGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(onboardingService.getGoal(userDetails.getUser().getId()), SuccessCode.OK);
    }

    // 독서 목표 단독 수정
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
