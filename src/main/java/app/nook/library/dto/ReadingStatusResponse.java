package app.nook.library.dto;

import app.nook.library.domain.enums.ReadingStatus;

public enum ReadingStatusResponse {
    BEFORE,
    READING,
    FINISHED,
    UNREGISTERED;

    public static ReadingStatusResponse from(ReadingStatus readingStatus) {
        return switch (readingStatus) {
            case BEFORE -> BEFORE;
            case READING -> READING;
            case FINISHED -> FINISHED;
        };
    }
}
