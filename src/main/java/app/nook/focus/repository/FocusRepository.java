package app.nook.focus.repository;

import app.nook.focus.domain.Focus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusRepository extends JpaRepository<Focus, Long>, FocusRepositoryCustom {
}
