package app.nook.r2.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MultipleImageUploadRequestDto(
        // 요청 제한 차감 전에 거부되도록 개수 상한을 DTO에서 검증 (PresignedUrlService.MAX_FILE_COUNT와 동일)
        @NotEmpty(message = "files는 최소 1개 이상이어야 합니다.")
        @Size(max = 5, message = "files는 최대 5개까지 요청할 수 있습니다.")
        @Valid
        List<ImageUploadRequestDto> files
) {
}
