package app.nook.library.converter;

import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;

import java.util.List;

public class LibraryConverter {

    private LibraryConverter() {}

    /* =========================
     * 단건 변환
     * ========================= */

    public static LibraryViewDto.UserStatusBookItem toStatusBookItem(Library library) {
        return switch (library.getReadingStatus()) {
            case BEFORE -> new LibraryViewDto.BeforeBookItem(
                    library.getBook().getId(),
                    library.getBook().getTitle(),
                    library.getBook().getAuthor(),
                    library.getBook().getCoverImageUrl()
            );
            case READING -> new LibraryViewDto.ReadingBookItem(
                    library.getBook().getId(),
                    library.getBook().getTitle(),
                    library.getBook().getAuthor(),
                    library.getBook().getCoverImageUrl(),
                    library.getStartedAt()
            );
            case FINISHED -> new LibraryViewDto.FinishedBookItem(
                    library.getBook().getId(),
                    library.getBook().getTitle(),
                    library.getBook().getAuthor(),
                    library.getBook().getCoverImageUrl(),
                    library.getStartedAt(),
                    library.getEndedAt()
            );
        };
    }

    /* =========================
     * 리스트 변환
     * ========================= */

    public static List<LibraryViewDto.UserStatusBookItem> toStatusBookItems(
            List<Library> libraries
    ) {
        return libraries.stream()
                .map(LibraryConverter::toStatusBookItem)
                .toList();
    }

    /* =========================
     * 커서 응답 생성
     * ========================= */

    public static CursorResponse<LibraryViewDto.UserStatusBookItem> toCursorResponse(
            List<Library> libraries,
            int size
    ) {
        boolean hasNext = libraries.size() > size;

        List<Library> content = hasNext
                ? libraries.subList(0, size)
                : libraries;

        Long nextCursor = hasNext
                ? content.get(content.size() - 1).getId()
                : null;

        return CursorResponse.of(
                toStatusBookItems(content),
                nextCursor,
                hasNext
        );
    }

    /* =========================
     * 최종 응답 DTO
     * ========================= */

    public static LibraryViewDto.StatusBookResponseDto toStatusBookResponse(
            ReadingStatus readingStatus,
            int totalBookNum,
            CursorResponse<LibraryViewDto.UserStatusBookItem> cursorResponse
    ) {
        return new LibraryViewDto.StatusBookResponseDto(
                readingStatus,
                totalBookNum,
                cursorResponse
        );
    }
}
