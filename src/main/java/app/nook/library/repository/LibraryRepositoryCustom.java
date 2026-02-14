package app.nook.library.repository;

import app.nook.library.domain.Library;
import app.nook.user.domain.User;

import java.time.YearMonth;
import java.util.List;

public interface LibraryRepositoryCustom {
    List<Library> findByUserAndYearMonth(User user, YearMonth yearMonth);
}
