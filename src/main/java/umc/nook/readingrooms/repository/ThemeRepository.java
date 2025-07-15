package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
}
