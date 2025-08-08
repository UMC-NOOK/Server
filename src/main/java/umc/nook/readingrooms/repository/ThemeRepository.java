package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.Theme;
import umc.nook.readingrooms.domain.ThemeName;

import java.util.Optional;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(ThemeName name);
}
