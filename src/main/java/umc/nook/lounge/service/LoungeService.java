package umc.nook.lounge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.repository.CategoryRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.dto.LoungeResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoungeService {

    private final AladinService aladinService;
    private final CategoryRepository categoryRepository;

    public LoungeResponseDTO.LoungeBookResultDTO getLoungeBooks(String mallType, String sectionId,
                                                                Integer categoryId, String queryType,
                                                                int page, int limit, String token) {

        // 파라미터 유효성 검사 필요
        // 카테고리 분류 필요
        // 에러 처리 필요

        List<LoungeResponseDTO.SectionDTO> sections = new ArrayList<>();

        if ("RECOMMENDATION".equalsIgnoreCase(mallType)) {
            if (sectionId == null) {
                // 1. 주간 베스트셀러
                sections.add(fetchSection("best", null, "", "BESTSELLER", "BOOK", page, limit));

                // 2. 개인 선호 카테고리
                // 카테고리 하드 코딩(추후에 선호 카테고리 기능 개발 예정)
                int favoriteCateogoryId = 170; // 국내도서>경제경영
                String favoriteCategoryName = getCategoryNameById(170);
                sections.add(fetchSection("favorite_best", favoriteCateogoryId, favoriteCategoryName,
                        "BESTSELLER", "BOOK", page, limit));
            } else if (sectionId.equalsIgnoreCase("best")) {
                // 1. 주간 베스트셀러
                sections.add(fetchSection(sectionId, null, "", queryType, "BOOK", page, limit));
            }
            else{
                // 2. 개인 선호 카테고리
                // 카테고리 하드 코딩(추후에 선호 카테고리 기능 개발 예정)
                int favoriteCateogoryId = 170; // 국내도서>경제경영
                String favoriteCategoryName = getCategoryNameById(170);
                sections.add(fetchSection(sectionId, favoriteCateogoryId, favoriteCategoryName,
                        queryType, "BOOK", page, limit));
            }
        }
        else {
            if (sectionId == null) {
                // 1. 신간
                sections.add(fetchSection("new", null, "",
                        "ITEMNEWALL", mallType, page, limit));

                // 2. mallType 별 주요 카테고리(하드코딩) 베스트셀러
                List<Integer> categoryIds = getCategoryIdsByMallType(mallType);
                for (Integer cid : categoryIds) {
                    String categoryName = getCategoryNameById(cid);
                    sections.add(fetchSection("best", cid, categoryName, "BESTSELLER",
                            mallType, page, limit));
                }
            } else if (sectionId.equalsIgnoreCase("new")) {
                // 1. 신간
                sections.add(fetchSection(sectionId, null, "",
                        "ITEMNEWALL", mallType, page, limit));
            }else{
                // 2. mallType 별 주요 카테고리(하드코딩) 베스트셀러
                sections.add(fetchSection(sectionId, categoryId, getCategoryNameById(categoryId), "BESTSELLER",
                        mallType, page, limit));
            }

        }
        return LoungeResponseDTO.LoungeBookResultDTO.builder()
                .sections(sections)
                .build();
    }

    private LoungeResponseDTO.SectionDTO fetchSection(
        String sectionId, Integer categoryId, String categoryName, String queryType,
        String searchTarget, int page, int limit) {
        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int start = (page - 1) * limit + 1;

        AladinResponseDTO.paginationDTO response = aladinService.fetchBooks(
                queryType, searchTarget, start, limit, categoryIdStr
        ).block();

        List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();


        if (response != null && response.getItem() != null) {
            for (AladinResponseDTO.loungeBookDTO item : response.getItem()) {
                if (isBookIncluded(item.getCategoryName())) {
                    books.add(LoungeResponseDTO.BookDTO.builder()
                            .isbn13(item.getIsbn13())
                            .title(item.getTitle())
                            .author(item.getAuthor())
                            .publisher(item.getPublisher())
                            .coverImageUrl(item.getCover())
                            .build());
                }
            }
        }


        int totalItems = response != null ? response.getTotalResults() : 0;
        int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / limit) : 0;

        LoungeResponseDTO.PaginationDTO pagination = LoungeResponseDTO.PaginationDTO.builder()
                .currentPage(page)
                .pageSize(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .build();

        return LoungeResponseDTO.SectionDTO.builder()
                .sectionId(sectionId)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .queryType(queryType)
                .books(books)
                .pagination(pagination)
                .build();

    }

    // 책의 카테고리가 도서 정책에 포함되는지 여부(true -> 포함 O, false -> 포함 x)
    private boolean isBookIncluded(String fullCategoryName) {
        // 1depth 제외 카테고리 맵
        Map<String, Set<String>> excluded1Depth = Map.of(
                "국내도서", Set.of("고등학교참고서", "수험서/자격증", "잡지", "중학교참고서", "초등학교참고서", "Gift"),
                "외국도서", Set.of("게임/토이", "달력/다이어리/연감", "문구/비도서", "수험서", "해외잡지"),
                "eBook", Set.of("19+", "가격대별 eBook", "고등학교참고서", "수험서/자격증", "잡지", "중고등참고서", "중학교참고서", "초등참고서", "Gift")
        );

        // 2depth 제외 조건 맵 (문자열 포함 여부)
        Map<String, Map<String, List<String>>> excluded2DepthContains = Map.of(
                "국내도서", Map.of(
                        "달력/기타", List.of("달력", "다이어리", "가계부")
                ),
                "외국도서", Map.of(
                        "독일 도서", List.of("CD/CD-ROM/DVD"),
                        "어린이", List.of("캐릭터"),
                        "일본 도서", List.of("애니메이션 굿즈", "엔터테인먼트", "잡지", "지브리 작품전", "캘린더", "CD/DVD"),
                        "중국 도서", List.of("CD/DVD/VCD")
                )
        );
        if (fullCategoryName != null && !fullCategoryName.isBlank()) {
            String[] parts = fullCategoryName.split(">");
            if (parts.length >= 2) {
                String mallType = parts[0].trim();
                String firstDepth = parts[1].trim();

                // 1depth 제외 체크
                Set<String> excludes1 = excluded1Depth.getOrDefault(mallType, Set.of());
                if (excludes1.contains(firstDepth)) {
                    return false;
                }

                // 2depth 문자열 포함 제외 체크
                if (parts.length >= 3) {
                    String secondDepth = parts[2].trim();
                    if (excluded2DepthContains.containsKey(mallType)) {
                        Map<String, List<String>> firstDepthMap = excluded2DepthContains.get(mallType);
                        if (firstDepthMap.containsKey(firstDepth)) {
                            for (String keyword : firstDepthMap.get(firstDepth)) {
                                if (secondDepth.contains(keyword)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    // 몰타입 별로 정해진 4개의 카테고리 정의
    private List<Integer> getCategoryIdsByMallType(String mallType) {
        if ("BOOK".equalsIgnoreCase(mallType)) {
            return List.of(170, 1, 656, 336); // 경제경영, 소설/시/희곡, 인문학, 자기계발
        } else if ("FOREIGN".equalsIgnoreCase(mallType)) {
            return List.of(90835, 90845, 90853, 90848); // 경제경영, 에세이, 인문/사회, 일본/문학 ? 예술/대중문화
        } else if ("EBOOK".equalsIgnoreCase(mallType)) {
            return List.of(38405,38416, 38396, 78871); // 과학, 만화, 소설/시/희곡, 판타지/무협
        }
        return List.of();
    }

    private String getCategoryNameById(Integer categoryId) {
        return categoryRepository.findCategoryNameByAladinCategoryId(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CATEGORY));
    }
}
