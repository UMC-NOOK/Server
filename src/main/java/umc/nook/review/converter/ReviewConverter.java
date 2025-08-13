package umc.nook.review.converter;

import org.springframework.data.domain.Page;
import umc.nook.review.domain.Review;
import umc.nook.review.dto.ReviewResponseDTO;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReviewConverter {

    public static ReviewResponseDTO.ReviewResultDTO toReviewResultDTO(Page<Review> reviewPage, User user) {
        List<ReviewResponseDTO.ReviewDTO> reviewDTOS = reviewPage.getContent().stream()
                .map(review -> toReviewDTO(review, user))
                .toList();

        return ReviewResponseDTO.ReviewResultDTO.builder()
                .reviews(reviewDTOS)
                .pagination(toPaginationDTO(reviewPage))
                .build();
    }

    public static ReviewResponseDTO.ReviewDTO toReviewDTO(Review review, User user) {
        User reviewUser = review.getUser();
        boolean ownedByUser = Objects.equals(reviewUser.getUserId(), user.getUserId());
        return ReviewResponseDTO.ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .name(reviewUser.getNickname())
                .nickname(reviewUser.getProfile().getAlias())
                .rating(review.getRating())
                .content(review.getContent())
                .reviewDate(review.getCreatedDate().toLocalDate())
                .ownedByUser(ownedByUser)
                .build();
    }

    public static ReviewResponseDTO.PaginationDTO toPaginationDTO(Page<Review> reviewPage) {
        return ReviewResponseDTO.PaginationDTO.builder()
                .currentPage(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalItems(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .build();
    }
}
