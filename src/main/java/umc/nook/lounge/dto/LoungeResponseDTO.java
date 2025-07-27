package umc.nook.lounge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class LoungeResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String coverImageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaginationDTO{
        private int currentPage;
        private int pageSize;
        private long totalItems;
        private int totalPages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SectionDTO{
        private String sectionId;
        private Integer categoryId;
        private String categoryName;
        private List<BookDTO> books;
        private PaginationDTO pagination;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoungeBookResultDTO{
        private List<SectionDTO> sections;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResultDTO{
        @Builder.Default
        List<CategoryDTO> categories = new ArrayList<>();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDTO{
        String categoryName;
        Long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalResultDTO{
        private String name;
        private String nickname;
        private int goal;
        private Long bookCount;
        private int dDay;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalRequestDTO{
        private int goal;
    }
}
