package app.nook.r2.dto;

import jakarta.validation.constraints.NotNull;

public record ImageUrlResponseDto (
        @NotNull(message = "url은 필수입니다.")
        String imageUrl,
        @NotNull(message = "key는 필수입니다.")
        String key
){}