package app.nook.r2.dto;

import java.util.List;

public record MultipleImageUploadRequestDto(
        List<ImageUploadRequestDto> files
) {
}
