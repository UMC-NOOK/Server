package app.nook.library.dto;

import app.nook.library.domain.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReadingStatusRequestDto(
        @NotNull
        @Positive
        Long bookId,
        @NotNull
        ReadingStatus readingStatus
){}
