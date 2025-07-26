package umc.nook.lounge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.domain.CategoryCount;
import umc.nook.book.domain.CategoryCountByName;
import umc.nook.book.repository.CategoryRepository;
import umc.nook.book.utils.BookFilterUtils;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.converter.LoungeConverter;
import umc.nook.lounge.dto.LoungeResponseDTO;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;

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
    private final UserBookshelfRepository userBookshelfRepository;
    private final CategoryRepository categoryRepository;

    public LoungeResponseDTO.LoungeBookResultDTO getLoungeBooks(
            String mallType, String sectionId, Integer categoryId, int page, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        // 추천 페이지
        if ("RECOMMENDATION".equalsIgnoreCase(mallType)) {
            return handleRecommendation(sectionId, categoryId, page, user);
        }
        // 몰 타입 페이지
        else {
            return handleMallType(sectionId, categoryId, mallType, page);
        }
    }

    private LoungeResponseDTO.SectionDTO fetchSection(
        String sectionId, Integer categoryId, String categoryName,String queryType,
        String mallType, int page) {

        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int start = (page - 1) * LIMIT + 1;

        AladinResponseDTO.ResultDTO response = aladinService.fetchBooks(queryType, mallType, start, LIMIT, categoryIdStr);
        List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();
        if (response != null && response.getItem() != null) {
            for (AladinResponseDTO.BookDetailDTO item : response.getItem()) {
                if (BookFilterUtils.isBookIncluded(item.getCategoryName())) {
                    books.add(LoungeConverter.toBookDTO(item));
                }
            }
        }

        int totalItems = response != null ? response.getTotalResults() : 0;
        int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / LIMIT) : 0;

        LoungeResponseDTO.PaginationDTO pagination = LoungeConverter.toPaginationDTO(
                page, LIMIT, totalItems, totalPages
        );

        return LoungeConverter.toSectionDTO(sectionId, categoryId, categoryName, books, pagination);
    }

    private LoungeResponseDTO.LoungeBookResultDTO handleRecommendation(
            String sectionId, Integer categoryId, int page, User user) {

        if (sectionId == null) {
            // 추천 페이지 전체 조회 (1페이지)
            LoungeResponseDTO.SectionDTO bestSection = getBestSection(SECTION_BEST, categoryId, page);
            int favoriteCategory = getFavoriteCategory(user).get(0).getAladinCategoryId();
            LoungeResponseDTO.SectionDTO favoriteSection = getBestSection(
                    SECTION_FAVORITE_BEST, favoriteCategory, page);

            return LoungeConverter.toResultDTO(List.of(bestSection, favoriteSection));
        } else if (sectionId.equalsIgnoreCase(SECTION_BEST)) {
            // 주간 베스트셀러의 특정 페이지 조회
            LoungeResponseDTO.SectionDTO section = getBestSection(SECTION_BEST, categoryId, page);
            return LoungeConverter.toResultDTO(List.of(section));
        } else {
            // 사용자 선호 카테고리 베스트셀러의 특정 페이지 조회
            int favoriteCategory = getFavoriteCategory(user).get(0).getAladinCategoryId();
            LoungeResponseDTO.SectionDTO section = getBestSection(
                    SECTION_FAVORITE_BEST, favoriteCategory, page);
            return LoungeConverter.toResultDTO(List.of(section));
        }
    }

    private LoungeResponseDTO.LoungeBookResultDTO handleMallType(
            String sectionId, Integer categoryId, String mallType, int page
    ) {
        if (sectionId == null) {
            // 몰 타입 페이지 전체 조회 (1 페이지)
            LoungeResponseDTO.SectionDTO newSection = getNewSection(categoryId, mallType, page);
            List<Integer> categoryIds = getCategoryIdsByMallType(mallType);
            List<LoungeResponseDTO.SectionDTO> bestSections = new ArrayList<>();
            for (Integer cid : categoryIds) {
                bestSections.add(getBestSection(SECTION_BEST, cid, page));
            }
            // newSection + 각 bestSection을 모아서 전달
            List<LoungeResponseDTO.SectionDTO> sections = new ArrayList<>();
            sections.add(newSection);
            sections.addAll(bestSections);
            return LoungeConverter.toResultDTO(sections);
        } else if (sectionId.equalsIgnoreCase(SECTION_NEW)) {
            // 신간의 특정 페이지 조회
            LoungeResponseDTO.SectionDTO section = getNewSection(categoryId, mallType, page);
            return LoungeConverter.toResultDTO(List.of(section));
        } else {
            // 몰 타입의 특정 카테고리 베스트셀러의 특정 페이지 조회
            LoungeResponseDTO.SectionDTO section = getBestSection(SECTION_BEST, categoryId, page);
            return LoungeConverter.toResultDTO(List.of(section));
        }
    }
    private LoungeResponseDTO.SectionDTO getBestSection(String sectionId, Integer categoryId, int page) {
        String categoryName = getCategoryNameById(categoryId);
        return fetchSection(sectionId, categoryId, categoryName, QUERY_TYPE_BESTSELLER, "BOOK", page);
    }

    private LoungeResponseDTO.SectionDTO getNewSection(Integer categoryId, String mallType, int page) {
        String categoryName = getCategoryNameById(categoryId);
        return fetchSection(SECTION_NEW, categoryId, categoryName, QUERY_TYPE_ITEMNEWALL, mallType, page);
    }

    // 사용자 선호 카테고리 추출
    // 추후 개발 예정
    private List<CategoryCount> getFavoriteCategory(User user) {
        Long count = userBookshelfRepository.countByUser_UserId(user.getUserId());
        if (count == 0L) {
            System.out.println("count == 0");
            return userBookshelfRepository.findCategoryCountGlobal(PageRequest.of(0, 1));
        }
        else{
            return userBookshelfRepository.findCategoryCountByUserId(
                    user.getUserId(), PageRequest.of(0, 1));
        }
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


    public LoungeResponseDTO.CategoryResultDTO getFavoriteCategories(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        Long totalCount = userBookshelfRepository.countByUser_UserId(user.getUserId());
        long topCountSum = 0L;

        List<CategoryCountByName> categories =
                userBookshelfRepository.findCategoryCountByUserIdGroupByName(
                        user.getUserId(), PageRequest.of(0, 5));

        LoungeResponseDTO.CategoryResultDTO categoryResultDTO = new LoungeResponseDTO.CategoryResultDTO();
        for (CategoryCountByName c : categories) {
            categoryResultDTO.getCategories().add(LoungeConverter.toCategoryDTO(c));
            topCountSum += c.getCount();
        }

        long otherCount = totalCount - topCountSum;
        if (otherCount > 0) {
            categoryResultDTO.getCategories().add(LoungeResponseDTO.CategoryDTO.builder()
                    .categoryName("기타")
                    .count(otherCount)
                    .build());
        }

        return categoryResultDTO;
    }
}
