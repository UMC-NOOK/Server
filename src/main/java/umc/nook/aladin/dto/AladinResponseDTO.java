package umc.nook.aladin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AladinResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class loungeBookDTO{
        private String isbn13;
        private String title;
        private String author;
        private String publisher;
        private String cover;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class paginationDTO{
        private int totalResults;
        private int startIndex;
        private int itemsPerPage;
        private List<loungeBookDTO> item;
    }
}
