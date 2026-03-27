package app.nook.r2.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MultipleImageUploadRequestDto(
        @NotEmpty(message = "files는 최소 1개 이상이어야 합니다.")
        @Valid
        List<ImageUploadRequestDto> files
) {
}
