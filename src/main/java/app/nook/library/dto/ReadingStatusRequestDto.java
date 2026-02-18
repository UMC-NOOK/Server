package app.nook.library.dto;

import app.nook.library.domain.enums.ReadingStatus;

public record ReadingStatusRequestDto(
        Long bookId,
        ReadingStatus readingStatus
){}
