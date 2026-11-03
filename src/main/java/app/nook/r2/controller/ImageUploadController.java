package app.nook.r2.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.ratelimit.RateLimitPolicy;
import app.nook.global.ratelimit.RateLimiter;
import app.nook.r2.dto.ImageUploadRequestDto;
import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.r2.dto.MultipleImageUploadRequestDto;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/images")
public class ImageUploadController {

    // 발급 장수 기준 분당·일일 한도를 함께 검사 (하나라도 넘으면 전체 거부)
    private static final List<RateLimitPolicy> IMAGE_POLICIES =
            List.of(RateLimitPolicy.IMAGE_MINUTE, RateLimitPolicy.IMAGE_DAY);

    private final PresignedUrlService presignedUrlService;
    private final RateLimiter rateLimiter;

    // 단건 업로드
    @PostMapping("/upload-url")
    public ApiResponse<ImageUrlResponseDto> issueUploadUrl(
            @CurrentUser User user,
            @Valid @RequestBody ImageUploadRequestDto requestDto
    ) {
        rateLimiter.consume(IMAGE_POLICIES, String.valueOf(user.getId()), 1);
        return ApiResponse.onSuccess(
                presignedUrlService.generateUploadUrl(user.getId(), requestDto),
                SuccessCode.OK
        );
    }

    // 다건 업로드
    @PostMapping("/upload-urls")
    public ApiResponse<List<ImageUrlResponseDto>> issueUploadUrls(
            @CurrentUser User user,
            @Valid @RequestBody MultipleImageUploadRequestDto requestDto
    ) {
        rateLimiter.consume(IMAGE_POLICIES, String.valueOf(user.getId()), requestDto.files().size());
        return ApiResponse.onSuccess(
                presignedUrlService.generateMultipleUploadUrls(user.getId(), requestDto.files()),
                SuccessCode.OK
        );
    }
}
