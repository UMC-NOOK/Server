package app.nook.book.repository;

import app.nook.book.entity.Category;
import app.nook.book.entity.MallType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByMallTypeAndCategoryName(MallType mallType, String categoryName);
}
