package app.nook.timeline.repository;

import app.nook.library.domain.Library;
import app.nook.timeline.domain.Timeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimelineRepository extends JpaRepository<Timeline, Long> {
    void deleteByLibrary(Library library);

    List<Timeline> findTop5ByLibraryOrderByOccurredAtDescIdDesc(Library library);

    List<Timeline> findByLibraryOrderByOccurredAtDescIdDesc(Library library);

    Optional<Timeline> findByIdAndLibrary(Long id, Library library);
}
