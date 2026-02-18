package app.nook.focus.repository;

import app.nook.focus.domain.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeRepository extends JpaRepository<Theme, Long> {

    List<Theme> findAllByOrderByIdAsc();
}
