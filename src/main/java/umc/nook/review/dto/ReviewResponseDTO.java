package umc.nook.review.dto;

public class ReviewResponseDTO {

    public static class ReviewDTO{
        private Long reviewId;
        private String name;
        private String nickname;
        private int rating;
        private String content;
        private String reviewDate;
        private boolean isOwnedByUser;
    }
}
