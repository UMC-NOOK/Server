package app.nook.library.repository.dto;

import app.nook.library.domain.enums.ReadingStatus;

import java.time.LocalDateTime;

public record LibraryBookQueryResult(
        Long libraryId,
        Long bookId,
        String title,
        String author,
        String coverImageKey,
        ReadingStatus readingStatus,
        LocalDateTime lastFocusedAt,
        Long recordCount
) {}
