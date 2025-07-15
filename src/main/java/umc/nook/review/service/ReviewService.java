package umc.nook.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.Book;
import umc.nook.book.repository.BookRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.review.converter.ReviewConverter;
import umc.nook.review.domain.Review;
import umc.nook.review.dto.ReviewRequestDTO;
import umc.nook.review.dto.ReviewResponseDTO;
import umc.nook.review.repository.ReviewRepository;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    public ReviewResponseDTO.ReviewResultDTO getReviews(String isbn13, CustomUserDetails userDetails, int page) {
        PageRequest pageable = PageRequest.of(page-1, 5, Sort.by("createdDate").descending());
        Page<Review> reviews = reviewRepository.findByBookIsbn13(isbn13, pageable);
        return ReviewConverter.toReviewResultDTO(reviews, userDetails.getUser());
    }

    @Transactional
    public ReviewResponseDTO.ReviewDTO addReview(
            String isbn13, ReviewRequestDTO.ReviewCreateDTO reviewDTO, CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        Book book = bookRepository.findByIsbn13(isbn13);

        Review review = Review.builder()
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .book(book)
                .user(user)
                .build();

        Review savedReview = reviewRepository.save(review);

        return ReviewConverter.toReviewDTO(savedReview, user);
    }

    @Transactional
    public ReviewResponseDTO.ReviewDTO modifyReview(
            Long reviewId, ReviewRequestDTO.ReviewCreateDTO reviewDTO, CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!Objects.equals(user.getUserId(), review.getUser().getUserId())) {
            throw new CustomException(ErrorCode.PERMISSION_DENIED);
        }
        review.modify(reviewDTO.getRating(), reviewDTO.getContent());

        return ReviewConverter.toReviewDTO(review, user);
    }

    @Transactional
    public void deleteReview(Long reviewId, CustomUserDetails userDetails) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!Objects.equals(userDetails.getUser().getUserId(), review.getUser().getUserId())) {
            throw new CustomException(ErrorCode.PERMISSION_DENIED);
        }
        reviewRepository.delete(review);
    }
}
