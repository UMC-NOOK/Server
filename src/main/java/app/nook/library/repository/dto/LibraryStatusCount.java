package app.nook.library.repository.dto;

import app.nook.library.domain.enums.ReadingStatus;

public record LibraryStatusCount(
        ReadingStatus readingStatus,
        long count
) {
}
