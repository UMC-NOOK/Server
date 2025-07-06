package umc.nook.lounge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.lounge.dto.LoungeResponseDTO;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoungeService {

    private final AladinService aladinService;

    public LoungeResponseDTO.LoungeBookResultDTO getLoungeBooks(String mallType, Integer categoryId,
                                                       String queryType, int page, int limit, String token) {

        List<LoungeResponseDTO.SectionDTO> sections = new ArrayList<>();

        if ("RECOMMENDATION".equalsIgnoreCase(mallType)) {
            // 1. 주간 베스트셀러
            sections.add(fetchSection(null, "", queryType, "BOOK", page, limit));

            // 2. 개인 선호 카테고리
            // 카테고리 하드 코딩(추후에 선호 카테고리 기능 개발 예정)
            int favoriteCateogoryId = 170; // 국내도서>경제경영
            String favoriteCategoryName = getCategoryNameById(170);
            sections.add(fetchSection(favoriteCateogoryId, favoriteCategoryName,
                    queryType, "BOOK", page, limit));
        }
        else {
            // 1. 신간
            sections.add(fetchSection(null, "",
                    "NEW", mallType, page, limit));

            // 2. mallType 별 주요 카테고리(하드코딩) 베스트셀러
            List<Integer> categoryIds = getCategoryIdsByMallType(mallType);
            for (Integer cid : categoryIds) {
                String categoryName = getCategoryNameById(cid);
                sections.add(fetchSection(cid, categoryName, "BESTSELLER",
                        mallType, page, limit));
            }
        }
        return LoungeResponseDTO.LoungeBookResultDTO.builder()
                .sections(sections)
                .build();
    }

    private LoungeResponseDTO.SectionDTO fetchSection(
        Integer categoryId, String categoryName, String queryType,
        String searchTarget, int page, int limit) {
        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int start = (page - 1) * limit + 1;

        AladinResponseDTO.paginationDTO response = aladinService.fetchBooks(
                queryType, searchTarget, start, limit, categoryIdStr
        ).block();

        List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();
        if (response != null && response.getItem() != null) {
            for (AladinResponseDTO.loungeBookDTO item : response.getItem()) {
                books.add(LoungeResponseDTO.BookDTO.builder()
                        .isbn13(item.getIsbn13())
                        .title(item.getTitle())
                        .author(item.getAuthor())
                        .publisher(item.getPublisher())
                        .coverImageUrl(item.getCover())
                        .build());
            }
        }

        LoungeResponseDTO.PaginationDTO pagination = LoungeResponseDTO.PaginationDTO.builder()
                .currentPage(page)
                .pageSize(limit)
                .totalItems(response != null && response.getTotalResults() > 0
                        ? (int) Math.ceil((double) response.getTotalResults() / limit) : 0)
                .build();

        return LoungeResponseDTO.SectionDTO.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .queryType(queryType)
                .books(books)
                .pagination(pagination)
                .build();

    }

    // mallType별 주요 카테고리(하드코딩)
    // 추후에 기능 추가 예정
    private List<Integer> getCategoryIdsByMallType(String mallType) {
        if ("BOOK".equalsIgnoreCase(mallType)) {
            return List.of(170, 1, 798); // 경제경영, 소설/시/희곡, 사회과학
        } else if ("FOREIGN".equalsIgnoreCase(mallType)) {
            return List.of(90835, 90842); // 경제경영, 소설/시/희곡
        } else if ("EBOOK".equalsIgnoreCase(mallType)) {
            return List.of(38398, 38396, 38404); // 경제경영, 소설/시/희곡, 사회과학
        }
        return List.of();
    }

    // 카테고리 이름 하드코딩
    // 추후에 기능 추가 예정
    private String getCategoryNameById(Integer categoryId) {
        return switch (categoryId) {
            case 170, 90835, 38398 -> "경제경영";
            case 1, 90842, 38396 -> "소설/시/희곡";
            case 798, 38404 -> "사회과학";
            default -> "";
        };
    }
}
