package app.nook.timeline.repository;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.BookTimeLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTimeLineRepository extends JpaRepository<BookTimeLine,Long> {
    BookTimeLine findByLibrary(Library library);
}
