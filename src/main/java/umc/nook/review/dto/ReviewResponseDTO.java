package umc.nook.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class ReviewResponseDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReviewDTO{
        private Long reviewId;
        private String name;
        private String nickname;
        private int rating;
        private String content;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate reviewDate;

        private boolean ownedByUser;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaginationDTO{
        private int currentPage;
        private int pageSize;
        private long totalItems;
        private int totalPages;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReviewResultDTO{
        private List<ReviewDTO> reviews;
        private PaginationDTO pagination;
    }


}
