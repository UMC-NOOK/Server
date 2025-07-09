package umc.nook.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import umc.nook.book.domain.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c.categoryName FROM Category c WHERE c.aladinCategoryId = :aladinCategoryId")
    Optional<String> findCategoryNameByAladinCategoryId(int aladinCategoryId);

    boolean existsByAladinCategoryId(int aladinCategoryId);
}
