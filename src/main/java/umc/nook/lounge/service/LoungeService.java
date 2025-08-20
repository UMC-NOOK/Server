package umc.nook.lounge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.domain.CategoryCount;
import umc.nook.book.domain.CategoryCountByName;
import umc.nook.book.domain.MallType;
import umc.nook.book.repository.CategoryRepository;
import umc.nook.book.utils.BookFilterUtils;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.lounge.converter.LoungeConverter;
import umc.nook.lounge.dto.LoungeResponseDTO;
import umc.nook.users.domain.User;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.CustomUserDetails;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class LoungeService {

    private static final String MALLTYPE_RECOMMENDATION= "RECOMMENDATION";
    private static final String MALLTYPE_BOOK = String.valueOf(MallType.BOOK);
    private static final String MALLTYPE_FOREIGN = String.valueOf(MallType.FOREIGN);
    private static final String MALLTYPE_EBOOK = String.valueOf(MallType.EBOOK);

    private static final String SECTION_BEST = "best";
    private static final String SECTION_NEW = "new";
    private static final String SECTION_FAVORITE_BEST = "favorite_best";

    private static final String QUERY_TYPE_BESTSELLER = "Bestseller";
    private static final String QUERY_TYPE_ITEMNEWALL = "ItemNewSpecial";

    private static final int LIMIT = 6;
    private static final int MAX_RESULT = 30;

    private final UserBookshelfRepository userBookshelfRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AladinService aladinService;

    public LoungeResponseDTO.LoungeBookResultDTO getLoungeBooks(
            String mallType,CustomUserDetails userDetails) {

        // 추천 페이지
        if (mallType.equalsIgnoreCase(MALLTYPE_RECOMMENDATION)) {
            return handleRecommendation(userDetails.getUser());
        }
        // 몰 타입 페이지
        else {
            return handleMallType(mallType);
        }
    }

    private LoungeResponseDTO.LoungeBookResultDTO handleRecommendation(User user) {

            // 추천 페이지 전체 조회)
            CompletableFuture<LoungeResponseDTO.SectionDTO> bestFuture =
                    fetchSectionAsync(SECTION_BEST, null, null,
                            QUERY_TYPE_BESTSELLER, MALLTYPE_BOOK);
            int favoriteCategory = getFavoriteCategory(user).get(0).getAladinCategoryId();
            CompletableFuture<LoungeResponseDTO.SectionDTO> favoriteFuture =
                    fetchSectionAsync(SECTION_FAVORITE_BEST, favoriteCategory,
                            getCategoryNameById(favoriteCategory), QUERY_TYPE_BESTSELLER,
                            MALLTYPE_BOOK);
            CompletableFuture.allOf(bestFuture, favoriteFuture).join();
            List<LoungeResponseDTO.SectionDTO> sections = List.of(
                    bestFuture.join(),
                    favoriteFuture.join()
            );

            return LoungeConverter.toResultDTO(sections);
    }

    private LoungeResponseDTO.LoungeBookResultDTO handleMallType(String mallType) {
            // 몰 타입 페이지 전체 조회
            LoungeResponseDTO.SectionDTO newSection = getNewSection(mallType);
            List<Integer> categoryIds = getCategoryIdsByMallType(mallType);
            List<CompletableFuture<LoungeResponseDTO.SectionDTO>> futures = categoryIds.stream()
                    .map(cid -> fetchSectionAsync(SECTION_BEST, cid, getCategoryNameById(cid),
                                    QUERY_TYPE_BESTSELLER, mallType))
                    .toList();
            List<LoungeResponseDTO.SectionDTO> bestSections = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            // newSection + 각 bestSection을 모아서 전달
            List<LoungeResponseDTO.SectionDTO> sections = new ArrayList<>();
            sections.add(newSection);
            sections.addAll(bestSections);
            return LoungeConverter.toResultDTO(sections);
    }
    private LoungeResponseDTO.SectionDTO getBestSection(String sectionId, Integer categoryId) {
        String categoryName = getCategoryNameById(categoryId);
        return fetchSection(sectionId, categoryId, categoryName, QUERY_TYPE_BESTSELLER, "BOOK");
    }

    private LoungeResponseDTO.SectionDTO getNewSection(String mallType) {
        return fetchSection(SECTION_NEW, null, null, QUERY_TYPE_ITEMNEWALL, mallType);
    }

    private LoungeResponseDTO.SectionDTO fetchSection(
            String sectionId, Integer categoryId, String categoryName,String queryType,
            String mallType) {

        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int totalPages = (categoryId != null && sectionId.equalsIgnoreCase(SECTION_BEST)) ? 2 : 3;
        int totalItems = LIMIT * totalPages;

        AladinResponseDTO.ResultDTO response = aladinService.fetchBooks(queryType, mallType, 1, MAX_RESULT, categoryIdStr);
        List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();
        if (response != null && response.getItem() != null) {
            for (AladinResponseDTO.BookDetailDTO item : response.getItem()) {
                if (BookFilterUtils.isValidBook(item)) {
                    books.add(LoungeConverter.toBookDTO(item));
                }
                if (books.size() >= totalItems) {
                    break;
                }
            }
        }
        return LoungeConverter.toSectionDTO(sectionId, categoryId, categoryName, books);
    }

    @Async("taskExecutor")
    public CompletableFuture<LoungeResponseDTO.SectionDTO> fetchSectionAsync(
            String sectionId, Integer categoryId, String categoryName, String queryType,
            String mallType
    ) {
        String categoryIdStr = (categoryId != null) ? String.valueOf(categoryId) : null;
        int totalPages = (categoryId != null && sectionId.equalsIgnoreCase(SECTION_BEST)) ? 2 : 3;
        int totalItems = LIMIT * totalPages;
        return aladinService.fetchBooksAsync(queryType, mallType, 1, MAX_RESULT, categoryIdStr)
                .handle((response, ex) -> {
                    List<LoungeResponseDTO.BookDTO> books = new ArrayList<>();
                    if (ex != null) {
                        return LoungeConverter.toSectionDTO(sectionId, categoryId, categoryName, books);
                    }
                    if (response != null && response.getItem() != null) {
                        for (AladinResponseDTO.BookDetailDTO item : response.getItem()) {
                            if (BookFilterUtils.isValidBook(item)) {
                                books.add(LoungeConverter.toBookDTO(item));
                            }
                            if (books.size() >= totalItems) {
                                break;
                            }
                        }
                    }
                    return LoungeConverter.toSectionDTO(sectionId, categoryId, categoryName, books);
                });
    }

    // 사용자 선호 카테고리 추출
    private List<CategoryCount> getFavoriteCategory(User user) {
        Long count = userBookshelfRepository.countByUser_UserId(user.getUserId());
        if (count == 0L) {
            List<CategoryCount> categoryCountGlobal =
                    userBookshelfRepository.findCategoryCountGlobal(PageRequest.of(0, 1));

            // 전체 사용자가 서재에 등록한 책이 0개일 경우
            if (categoryCountGlobal.isEmpty()) {
                // 국내도서 소설/시/희곡을 기본 카테고리로 설정
                LoungeResponseDTO.CategoryCountDTO categoryCountDTO =
                        new LoungeResponseDTO.CategoryCountDTO(
                                10L, 1, "소설/시/희곡", 0L);
                return List.of(categoryCountDTO);
            }
            return categoryCountGlobal;
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
            return List.of(90835, 90845, 90853, 90842); // 경제경영, 에세이, 인문/사회, 소설/시/희곡
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

    // 독서 목표 조회
    public LoungeResponseDTO.GoalResultDTO getGoal(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<ReadingStatus> readingStatuses = List.of(ReadingStatus.READING, ReadingStatus.FINISHED);
        LocalDate today = LocalDate.now();
        LocalDate startDate = LocalDate.of(today.getYear(), 1, 1);
        LocalDate endDate = LocalDate.of(today.getYear(), 12, 31);
        long bookCountOfYear = userBookshelfRepository.countByUserAndReadingStatusInAndRecordedAtBetween(
                user, readingStatuses, startDate, endDate);

        return LoungeResponseDTO.GoalResultDTO.builder()
                .name(user.getNickname())
                .nickname(user.getProfile().getAlias())
                .goal(user.getGoal())
                .bookCount(bookCountOfYear)
                .build();
    }

    // 독서 목표 수정
    @Transactional
    public void modifyGoal(CustomUserDetails userDetails, LoungeResponseDTO.GoalRequestDTO goalRequestDTO) {
        Set<Integer> ALLOWED_GOALS = Set.of(50, 100, 150, 200, 250, 300);

        if (!ALLOWED_GOALS.contains(goalRequestDTO.getGoal())) {
            throw new CustomException(ErrorCode.INVALID_GOAL_VALUE);
        }

        User user = userRepository.findByUserId(userDetails.getUser().getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.setGoal(goalRequestDTO.getGoal());
    }
}
