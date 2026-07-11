package app.nook.library.repository;

import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.repository.dto.LibraryBookQueryResult;

import java.util.List;

public interface LibraryQueryRepository {
    List<LibraryBookQueryResult> findAllBooksByCursor(
            Long userId, LibraryBookCursor cursor, LibrarySortType sortType, int size
    );
}
