package app.nook.aladin.converter;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.entity.MallType;


public class AladinConverter {
    public static BookResponseDto.BookPreviewDto toBookPreviewDto(AladinResponseDto.AladinItem aladinItem) {
        return BookResponseDto.BookPreviewDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(aladinItem.getTitle())
                .author(aladinItem.getAuthor())
                .coverImageUrl(aladinItem.getCover())
                .publisher(aladinItem.getPublisher())
                .rank(aladinItem.getBestRank())
                .build();
    }
    public static BookResponseDto.BookDetailDto toBookDetailDto(AladinResponseDto.AladinItem aladinItem) {
        CategoryResult categoryResult = parseCategory(aladinItem.getCategoryName());

        return BookResponseDto.BookDetailDto.builder()
                .isbn13(aladinItem.getIsbn13())
                .title(aladinItem.getTitle())
                .author(aladinItem.getAuthor())
                .mallType(categoryResult.mallTypeEnum.getDisplayName())
                .mallTypeCode(categoryResult.mallTypeEnum)
                .category(categoryResult.category)
                .publisher(aladinItem.getPublisher())
                .publicationDate(aladinItem.getPubDate())
                .pages(aladinItem.getItemPage())
                .description(aladinItem.getDescription())
                .coverImageUrl(aladinItem.getCover())
                .aladinLink(aladinItem.getLink())
                .build();
    }

    private static CategoryResult parseCategory(String rawCategory) {
        // null이 오면 "기타"로 처리
        if (rawCategory == null || rawCategory.isBlank()) {
            return new CategoryResult(MallType.ETC, "기타");
        }

        // ">" 기준으로 자르기
        String[] parts = rawCategory.split(">");

        // 1 Depth 파싱: "외국도서" -> MallType.FOREIGN 변환
        String firstPart = parts.length > 0 ? parts[0] : "";
        MallType mallType = MallType.fromDisplayName(firstPart);

        // 2 Depth 파싱: "어린이" (없으면 "기타")
        String category = parts.length > 1 ? parts[1].trim() : "기타";

        return new CategoryResult(mallType, category);
    }

    // 카테고리 파싱 결과를 담을 내부 클래스
    private record CategoryResult(MallType mallTypeEnum, String category) {}
}
