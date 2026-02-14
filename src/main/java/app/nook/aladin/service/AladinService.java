package app.nook.aladin.service;

import app.nook.aladin.converter.AladinConverter;
import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.exception.AladinErrorCode;
import app.nook.aladin.utils.AladinUtils;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.domain.enums.BookCategory;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AladinService {

    private final RestClient restClient;

    @Value("${aladin.ttbkey}")
    private String ttbKey;

    private static final String ALADIN_BASE_URL = "https://www.aladin.co.kr/ttb/api";

    private static final String ALADIN_LIST_PATH = "/ItemList.aspx";
    private static final String ALADIN_SEARCH_PATH = "/ItemSearch.aspx";
    private static final String ALADIN_LOOKUP_PATH = "/ItemLookUp.aspx";

    // 도서 목록 조회
    public List<BookResponseDto.BookPreviewDto> fetchItemList(
            String queryType,   // 예: Bestseller, ItemNewAll
            String searchTarget,// 예: Book, Foreign
            int targetCount,     // 예: 5, 10
            String categoryId   // Nullable
    ) {
        // API 요청 개수 설정 (오버 페칭)
        // 필요한 개수의 4배수를 요청하되, 알라딘 최대치(50)를 넘지 않도록 설정
        int apiRequestCount = Math.min(targetCount * 4, 50);
        UriComponentsBuilder uriBuilder = getBaseUriBuilder(ALADIN_LIST_PATH)
                .queryParam("QueryType", queryType)
                .queryParam("SearchTarget", searchTarget)
                .queryParam("MaxResults", apiRequestCount);

        if (categoryId != null && !categoryId.isBlank()) {
            uriBuilder.queryParam("CategoryId", categoryId);
        }
        try {
            AladinResponseDto.AladinApiResult apiResult = execute(uriBuilder.build().toUri());
            List<AladinResponseDto.AladinItem> rawItems = apiResult.getItem() != null
                    ? apiResult.getItem() : Collections.emptyList();

            List<BookResponseDto.BookPreviewDto> result = new ArrayList<>();
            int currentRank = 1;

            for (AladinResponseDto.AladinItem item: rawItems) {
                if (!AladinUtils.isValid(item)) {
                    continue;
                }
                result.add(AladinConverter.toBookPreviewDto(item, currentRank++));

                if (result.size() >= targetCount) {
                    break;
                }
            }

            // 데이터가 부족한 경우 경고
            if (result.size() < targetCount) {
                log.warn("[FETCH_WARNING] reason='Insufficient count', target={}, fetched={}",
                        targetCount, result.size());
            }

            return result;

        } catch (CustomException e) {
            log.error("[FETCH_ERROR] message='{}'", e.getMessage());
            throw new CustomException(AladinErrorCode.ALADIN_API_ERROR);
        }
    }

    /**
     * 커서 기반 무한스크롤 검색
     * Global Index 기반 (0~49: 1페이지, 50~99: 2페이지 ...)
     * size만큼 채워질 때까지 다음 페이지를 계속 조회함
     */
    // TODO: redis 도입 예정
    public BookResponseDto.SearchResultDto searchItems(String keyword, Integer cursor, int size) {
        List<BookResponseDto.BookSearchDto> results = new ArrayList<>();
        Long totalResults = 0L;
        // 1. 현재 탐색중인 전역 인덱스 (null이면 0부터 시작)
        int currentGlobalIndex = (cursor == null) ? 0 : cursor;

        // 알라딘 최대 조회 개수
        final int ALADIN_MAX_RESULTS = 50;

        // 무한 루프 방지 (최대 5페이지까지만 조회)
        int maxPageSearchLimit = 5;
        int pageSearchCount = 0;

        // 2. 목표 개수(size)를 채울 때까지 반복
        while (results.size() < size && pageSearchCount < maxPageSearchLimit) {
            // 2-1. 현재 페이지 계산
            int pageNum = (currentGlobalIndex / ALADIN_MAX_RESULTS) + 1; // 1부터 시작
            int startIndex = currentGlobalIndex % ALADIN_MAX_RESULTS; // 페이지 내 시작 인덱스 (0 ~ 49)

            // 2-2. 알라딘 API 요청
            URI uri = getBaseUriBuilder(ALADIN_SEARCH_PATH)
                    .queryParam("Query", keyword)
                    .queryParam("SearchTarget", "All")
                    .queryParam("MaxResults", ALADIN_MAX_RESULTS)
                    .queryParam("Start", pageNum)
                    .build()
                    .toUri();

            List<AladinResponseDto.AladinItem> rawItems;
            AladinResponseDto.AladinApiResult apiResult;
            try {
                apiResult = execute(uri);
                rawItems = apiResult.getItem() != null ? apiResult.getItem() : Collections.emptyList();
            } catch (Exception e) {
                log.error("[SEARCH_ERROR] message='{}'", e.getMessage());
                throw new CustomException(AladinErrorCode.ALADIN_API_ERROR);
            }
            if (apiResult.getTotalResults() != null) {
                totalResults = apiResult.getTotalResults();
            }

            // 더 이상 검색 결과가 없는 경우 종료
            if (rawItems.isEmpty()) {
                break;
            }

            int filteredCount = 0;

            for (int i = startIndex; i < rawItems.size(); i++) {
                currentGlobalIndex++;
                AladinResponseDto.AladinItem item = rawItems.get(i);

                if (!AladinUtils.isValid(item)) {
                    filteredCount++;
                    continue;
                }

                results.add(AladinConverter.toBookSearchDto(item));

                if (results.size() >= size) {
                    break;
                }
            }

            if (filteredCount > 0) {
                log.debug("[FILTERING] page={}, filteredCount={}", pageNum, filteredCount);
            }

            // 이번 페이지를 다 돌았는데도 size가 안 찼다면?
            // while 문 조건에 의해 다시 위로 올라감 -> currentGlobalIndex는 이미 증가했으므로 자동으로 다음 페이지 계산됨
            // 예: 49번(1페이지 끝)까지 봤으면 currentGlobalIndex=50이 됨 -> 다음 루프에서 pageNum=2로 계산됨.
            pageSearchCount++;

            // 현재 페이지가 마지막 페이지인 경우 종료
            if (rawItems.size() < ALADIN_MAX_RESULTS) {
                break;
            }
        }

        Integer nextCursor = null;

        if (!results.isEmpty() && currentGlobalIndex < totalResults) {
            nextCursor = currentGlobalIndex;
        }
        if (pageSearchCount >= maxPageSearchLimit) {
            log.warn("[MAX_LIMIT_REACHED] keyword='{}', limit={}, fetched={}",
                    keyword, maxPageSearchLimit, results.size());
        }
        return new BookResponseDto.SearchResultDto(
                totalResults,
                nextCursor != null,
                nextCursor,
                results
        );
    }

    // 도서 상세 정보 조회
    public BookResponseDto.BookDetailDto lookupItem(String isbn13) {
        URI uri = getBaseUriBuilder(ALADIN_LOOKUP_PATH)
                .queryParam("ItemId", isbn13)
                .queryParam("ItemIdType", "ISBN13") // 혹은 파라미터화
                .build()
                .toUri();
        AladinResponseDto.AladinApiResult apiResult = execute(uri);
        List<AladinResponseDto.AladinItem> items = apiResult.getItem() != null
                ? apiResult.getItem() : Collections.emptyList();

        // ISBN13에 해당하는 도서가 없는 경우
        if (items.isEmpty()) {
            throw new CustomException(BookErrorCode.ISBN13_NOT_FOUND);
        }

        AladinResponseDto.AladinItem item = items.get(0);
        if (!AladinUtils.isValid(item)) {
            log.info("[LOOKUP_INVALID] isbn={}, title='{}'", isbn13, item.getTitle());
            throw new CustomException(BookErrorCode.BOOK_NOT_ALLOWED);
        }

        String rawCategoryName = AladinUtils.extractCategoryName(item.getCategoryName());
        BookCategory bookCategory = BookCategory.match(rawCategoryName)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_ALLOWED));
        String dbCategoryName = bookCategory.getDbName();

        return AladinConverter.toBookDetailDto(item, dbCategoryName);
    }

    // API 호출 및 응답 파싱 공통 메서드
    private AladinResponseDto.AladinApiResult execute(URI uri) {
        log.debug("[API_REQUEST] url={}", uri);
        try {
            AladinResponseDto.AladinApiResult response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(AladinResponseDto.AladinApiResult.class);

            if (response == null) {
                log.info("[API_EMPTY] url={}", uri);
                return new AladinResponseDto.AladinApiResult();
            }

            return response;

        } catch (Exception e) {
            log.error("[API_ERROR] url={}, message='{}'", uri, e.getMessage());
            throw new CustomException(AladinErrorCode.ALADIN_API_ERROR);
        }
    }

    // 공통 URL 빌더
    private UriComponentsBuilder getBaseUriBuilder(String path) {
        return UriComponentsBuilder.fromUriString(ALADIN_BASE_URL)
                .path(path)
                .queryParam("ttbkey", ttbKey)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101");
    }

}
