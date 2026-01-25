package app.nook.aladin.service;

import app.nook.aladin.converter.AladinConverter;
import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.util.AladinUtils;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.entity.BookCategory;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
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
import java.util.Objects;
import java.util.stream.Stream;

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
            List<AladinResponseDto.AladinItem> rawItems = execute(uriBuilder.build().toUri());

            List<AladinResponseDto.AladinItem> validItems = rawItems.stream()
                    .filter(item -> !item.isAdult()) // 19금 필터링
                    .filter(item -> {
                        // 카테고리 매핑 확인
                        String rawCategoryName = AladinUtils.extractCategoryName(item.getCategoryName());
                        return BookCategory.match(rawCategoryName).isPresent();
                    })
                    .limit(targetCount)
                    .toList();

            List<BookResponseDto.BookPreviewDto> result = new ArrayList<>();

            for (int i = 0; i < validItems.size(); i++) {
                AladinResponseDto.AladinItem item = validItems.get(i);
                result.add(AladinConverter.toBookPreviewDto(item, i + 1));
            }

            // 데이터가 부족한 경우 경고
            if (result.size() < targetCount) {
                log.warn("[AladinService] 목표 개수 부족! 요청: {}, 확보: {} (필터링 비율이 예상보다 높음)",
                        targetCount, result.size());
            }

            return result;

        } catch (CustomException e) {
            log.error("[AladinService] 도서 목록 조회 실패: {} -> 빈 리스트 반환", e.getMessage());
            return Collections.emptyList();
        }
    }

    // 도서 검색
//    public List<BookResponseDto.BookPreviewDto> searchItems(String query, int start, int maxResults) {
//        URI uri = getBaseUriBuilder(ALADIN_SEARCH_PATH)
//                .queryParam("Query", query)
//                .queryParam("Start", start)
//                .queryParam("MaxResults", maxResults)
//                .build()
//                .toUri();
//
//        return execute(uri);
//    }

    // 도서 상세 정보 조회
    public BookResponseDto.BookDetailDto lookupItem(String isbn13) {
        URI uri = getBaseUriBuilder(ALADIN_LOOKUP_PATH)
                .queryParam("ItemId", isbn13)
                .queryParam("ItemIdType", "ISBN13") // 혹은 파라미터화
                .build()
                .toUri();
        List<AladinResponseDto.AladinItem> items = execute(uri);

        // ISBN13에 해당하는 도서가 없는 경우
        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.ISBN13_NOT_FOUND);
        }
        AladinResponseDto.AladinItem item = items.get(0);

        // 19금 도서인 경우 예외 처리
        if(item.isAdult()) {
            throw new CustomException(ErrorCode.BOOK_NOT_ALLOWED);
        }

        String rawCategoryName = parseMainCategory(item.getCategoryName());
        // 카테고리 매핑 확인
        BookCategory bookCategory = BookCategory.match(rawCategoryName)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_ALLOWED));
        return AladinConverter.toBookDetailDto(item, bookCategory.getDbName());
    }

    // API 호출 및 응답 파싱 공통 메서드
    private List<AladinResponseDto.AladinItem> execute(URI uri) {
        try {
            AladinResponseDto.AladinApiResult response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(AladinResponseDto.AladinApiResult.class);

            if (isEmpty(response)) {
                log.info("[AladinClient] 응답 결과 없음. URI={}", uri);
                return Collections.emptyList();
            }

            return response.getItem();

        } catch (Exception e) {
            log.error("[AladinClient] API 호출 실패: URL={}, Error={}", uri, e.getMessage());
            throw new CustomException(ErrorCode.ALADIN_API_ERROR);
        }
    }

    private boolean isEmpty(AladinResponseDto.AladinApiResult response) {
        return response == null || response.getItem() == null;
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
    /**
     * 알라딘 카테고리 문자열에서 1depth만 추출
     * 예: "국내도서>소설/시/희곡>한국소설" -> "소설/시/희곡"
     */
    private String parseMainCategory(String fullCategoryString) {
        if (fullCategoryString == null || fullCategoryString.isBlank()) {
            return "";
        }
        String[] parts = fullCategoryString.split(">");
        // parts[0]: "국내도서" or "전자책"
        // parts[1]: "소설/시/희곡"
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return ""; // 형식이 맞지 않으면 빈 문자열 -> 필터링
    }
}
