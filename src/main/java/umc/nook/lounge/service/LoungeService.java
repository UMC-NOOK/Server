package umc.nook.lounge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.domain.MallType;
import umc.nook.book.repository.CategoryRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.converter.LoungeConverter;
import umc.nook.lounge.dto.LoungeResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoungeService {

    private static final String SECTION_BEST = "best";
    private static final String SECTION_NEW = "new";
    private static final String SECTION_FAVORITE_BEST = "favorite_best";

    private static final String QUERY_TYPE_BESTSELLER = "BESTSELLER";
    private static final String QUERY_TYPE_ITEMNEWALL = "ITEMNEWALL";

    private static final int LIMIT = 6;

    private final AladinService aladinService;
    private final CategoryRepository categoryRepository;

    public Mono<LoungeResponseDTO.LoungeBookResultDTO> getLoungeBooks(
            String mallType, String sectionId, Integer categoryId, int page, String token) {

        // 추천 페이지
        if ("RECOMMENDATION".equalsIgnoreCase(mallType)) {
            return handleRecommendation(sectionId, categoryId, page, token);
        }
        // 몰 타입 페이지
        else {
            return handleMallType(sectionId, categoryId, mallType, page);
        }
    }

    private Mono<LoungeResponseDTO.SectionDTO> fetchSection(
        String sectionId, Integer categoryId, String categoryName,String queryType,
        String mallType, int page) {

        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int start = (page - 1) * LIMIT + 1;

        return aladinService.fetchBooks(queryType, mallType, start, LIMIT, categoryIdStr)
                .map(response -> {
                    List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();
                    if (response != null && response.getItem() != null) {
                        for (AladinResponseDTO.LoungeBookDTO item : response.getItem()) {
                            if (isBookIncluded(item.getCategoryName())) {
                                books.add(LoungeConverter.toBookDTO(item));
                            }
                        }
                    }

                    int totalItems = response != null ? response.getTotalResults() : 0;
                    int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / LIMIT) : 0;

                    LoungeResponseDTO.PaginationDTO pagination = LoungeConverter.toPaginiationDTO(
                            page, LIMIT, totalItems, totalPages
                    );

                    return LoungeConverter.toSectionDTO(sectionId, categoryId, categoryName, books, pagination);
                });
    }

    private Mono<LoungeResponseDTO.LoungeBookResultDTO> handleRecommendation(
            String sectionId, Integer categoryId, int page, String token) {

        if (sectionId == null) {
            // 추천 페이지 전체 조회 (1페이지)
            Mono<LoungeResponseDTO.SectionDTO> bestSection = getBestSection(SECTION_BEST, categoryId, page);
            Mono<LoungeResponseDTO.SectionDTO> favoriteSection = getBestSection(SECTION_FAVORITE_BEST, getFavoriteCategory(token), page);

            return Mono.zip(bestSection, favoriteSection)
                    .map(tuple -> LoungeConverter.toResultDTO(
                            List.of(tuple.getT1(), tuple.getT2())
                    ));
        }
        else if (sectionId.equalsIgnoreCase(SECTION_BEST)) {
            // 주간 베스트셀러의 특정 페이지 조회
            return getBestSection(SECTION_BEST, categoryId, page)
                    .map(section -> LoungeConverter.toResultDTO(List.of(section)));
        }
        else{
            // 사용자 선호 카테고리 베스트셀러의 특정 페이지 조회
            return getBestSection(SECTION_FAVORITE_BEST, getFavoriteCategory(token), page)
                    .map(section -> LoungeConverter.toResultDTO(List.of(section)));
        }
    }

    private Mono<LoungeResponseDTO.LoungeBookResultDTO> handleMallType(
            String sectionId, Integer categoryId, String mallType, int page
    ) {
        if (sectionId == null) {
            // 몰 타입 페이지 전체 조회 (1 페이지)
            Mono<LoungeResponseDTO.SectionDTO> newSection = getNewSection(categoryId, mallType, page);
            List<Integer> categoryIds = getCategoryIdsByMallType(mallType);
            List<Mono<LoungeResponseDTO.SectionDTO>> bestSections = new ArrayList<>();
            for (Integer cid : categoryIds) {
                bestSections.add(getBestSection(SECTION_BEST, cid, page));
            }
            return Mono.zip(newSection, Flux.merge(bestSections).collectList())
                    .map(tuple -> {
                        List<LoungeResponseDTO.SectionDTO> sections = new ArrayList<>();
                        sections.add(tuple.getT1());
                        sections.addAll(tuple.getT2());
                        return LoungeConverter.toResultDTO(sections);
                    });
        }
        else if (sectionId.equalsIgnoreCase("new")) {
            // 신간의 특정 페이지 조회
            return getNewSection(categoryId, mallType, page)
                    .map(section -> LoungeConverter.toResultDTO(List.of(section)));
        }
        else{
            // 몰 타입의 특정 카테고리 베스트셀러의 특정 페이지 조회
            return getBestSection(SECTION_BEST, categoryId, page)
                    .map(section -> LoungeConverter.toResultDTO(List.of(section)));
        }
    }
    private Mono<LoungeResponseDTO.SectionDTO> getBestSection(String sectionId, Integer categoryId, int page) {
        String categoryName = getCategoryNameById(categoryId);
        return fetchSection(sectionId, categoryId, categoryName, QUERY_TYPE_BESTSELLER, "BOOK", page);
    }

    private Mono<LoungeResponseDTO.SectionDTO> getNewSection(Integer categoryId, String mallType, int page) {
        String categoryName = getCategoryNameById(categoryId);
        return fetchSection(SECTION_NEW, categoryId, categoryName, QUERY_TYPE_ITEMNEWALL, mallType, page);
    }

    // 사용자 선호 카테고리 추출
    // 추후 개발 예정
    private int getFavoriteCategory(String token) {
        return 170; // 하드 코딩
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
                String mallType = parts[0].trim(); // 몰타입 추출
                String firstDepth = parts[1].trim(); // 1depth 추출

                // 1depth 제외 체크
                Set<String> excludes1 = excluded1Depth.getOrDefault(mallType, Set.of());
                if (excludes1.contains(firstDepth)) {
                    return false;
                }

                // 2depth 문자열 포함 제외 체크
                if (parts.length >= 3) {
                    String secondDepth = parts[2].trim(); // 2depth 추출
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
        if ("BOOK".equalsIgnoreCase(mallType)) {// 국내도서
            return List.of(170, 1, 656, 336); // 경제경영, 소설/시/희곡, 인문학, 자기계발
        } else if ("FOREIGN".equalsIgnoreCase(mallType)) { // 외국도서
            return List.of(90835, 90845, 90853, 90848); // 경제경영, 에세이, 인문/사회, 일본/문학 ? 예술/대중문화
        } else if ("EBOOK".equalsIgnoreCase(mallType)) { // 전자책
            return List.of(38405,38416, 38396, 78871); // 과학, 만화, 소설/시/희곡, 판타지/무협
        }
        return List.of();
    }

    private String getCategoryNameById(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findCategoryNameByAladinCategoryId(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CATEGORY));
    }
}
