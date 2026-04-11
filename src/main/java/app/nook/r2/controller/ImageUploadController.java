package app.nook.r2.controller;

import app.nook.global.api.Api1Version;
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

    private final PresignedUrlService presignedUrlService;

    // 단건 업로드
    @PostMapping("/upload-url")
    public ApiResponse<ImageUrlResponseDto> issueUploadUrl(
            @CurrentUser User user,
            @Valid @RequestBody ImageUploadRequestDto requestDto
    ) {
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
        return ApiResponse.onSuccess(
                presignedUrlService.generateMultipleUploadUrls(user.getId(), requestDto.files()),
                SuccessCode.OK
        );
    }
}
