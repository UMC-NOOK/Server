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

    public ReviewResponseDTO.ReviewResultDTO getReviews(Long bookId, CustomUserDetails userDetails, int page) {
        PageRequest pageable = PageRequest.of(page, 5, Sort.by("createdDate").descending());
        Page<Review> reviews = reviewRepository.findByBookBookId(bookId, pageable);
        if (page >= reviews.getTotalPages()) {
            throw new CustomException(ErrorCode.PAGE_OUT_OF_RANGE);
        }
        return ReviewConverter.toReviewResultDTO(reviews, userDetails.getUser());
    }

    @Transactional
    public ReviewResponseDTO.ReviewDTO addReview(
            Long bookId, ReviewRequestDTO.ReviewCreateDTO reviewDTO, CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        if (reviewRepository.existsByUserUserIdAndBookBookId(user.getUserId(), bookId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Book book = bookRepository.findByBookId(bookId);

            if (book == null) {
                throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
            }

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
