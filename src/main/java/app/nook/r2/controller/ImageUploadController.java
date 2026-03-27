package app.nook.r2.controller;

import app.nook.r2.dto.ImageUploadRequestDto;
import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.r2.dto.MultipleImageUploadRequestDto;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageUploadController {

    private final PresignedUrlService presignedUrlService;

    // 단건 업로드
    @PostMapping("/upload-url")
    public ApiResponse<ImageUrlResponseDto> issueUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ImageUploadRequestDto requestDto
    ) {
        return ApiResponse.onSuccess(
                presignedUrlService.generateUploadUrl(userDetails.getUser().getId(), requestDto),
                SuccessCode.OK
        );
    }

    // 다건 업로드
    @PostMapping("/upload-urls")
    public ApiResponse<List<ImageUrlResponseDto>> issueUploadUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MultipleImageUploadRequestDto requestDto
    ) {
        return ApiResponse.onSuccess(
                presignedUrlService.generateMultipleUploadUrls(userDetails.getUser().getId(), requestDto.files()),
                SuccessCode.OK
        );
    }
}
