package app.nook.user.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/users/me/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    // 온보딩 최종 완료(프로필 이미지 포함)
    @PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OnboardingDto.CompleteResponse> completeOnboarding(
            @CurrentUser User user,
            @Valid @ModelAttribute OnboardingDto.CompleteRequest request
    ) {
        return ApiResponse.onSuccess(onboardingService.completeOnboarding(user.getId(), request), SuccessCode.OK);
    }

    // 온보딩 완료 여부 조회
    @GetMapping("/status")
    public ApiResponse<OnboardingDto.StatusResponse> getOnboardingStatus(
            @CurrentUser User user
    ) {
        return ApiResponse.onSuccess(
                onboardingService.getOnboardingStatus(user.getId()),
                SuccessCode.OK
        );
    }

    // 독서 목표/남은 권수 조회
    @GetMapping("/goal")
    public ApiResponse<OnboardingDto.GoalResponse> getGoal(
            @CurrentUser User user) {
        return ApiResponse.onSuccess(onboardingService.getGoal(user.getId()), SuccessCode.OK);
    }

    // 독서 목표 단독 수정
    @PatchMapping("/goal")
    public ApiResponse<OnboardingDto.GoalUpdateResponse> updateGoal(
            @CurrentUser User user,
            @Valid @RequestBody OnboardingDto.GoalUpdateRequest request
    ) {
        return ApiResponse.onSuccess(
                onboardingService.updateGoal(user.getId(), request),
                SuccessCode.OK
        );
    }
}
