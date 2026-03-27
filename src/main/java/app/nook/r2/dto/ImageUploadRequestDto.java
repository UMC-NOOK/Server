package app.nook.r2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ImageUploadRequestDto(
        @NotBlank(message = "contentType은 필수입니다.")
        @Pattern(
                regexp = "^(book|record|profile)$",
                message = "contentType은 book, record, profile 중 하나여야 합니다."
        )
        String contentType
) {
}
