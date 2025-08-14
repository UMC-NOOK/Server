package umc.nook.review.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.review.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "user.profile"})
    Page<Review> findByBookBookId(Long BookId, Pageable pageable);

    boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);
}
