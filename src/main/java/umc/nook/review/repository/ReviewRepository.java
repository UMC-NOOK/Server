package umc.nook.review.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.review.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByBookIsbn13(String isbn13, Pageable pageable);
}
