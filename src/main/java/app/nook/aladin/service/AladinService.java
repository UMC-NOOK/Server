package app.nook.aladin.service;

import app.nook.aladin.converter.AladinConverter;
import app.nook.aladin.dto.AladinResponseDto;
import app.nook.book.dto.BookResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AladinService {
    private final RestClient restClient = RestClient.create();

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
            int maxResults,     // 예: 5, 10
            String categoryId   // Nullable
    ) {
        UriComponentsBuilder uriBuilder = getBaseUriBuilder(ALADIN_LIST_PATH)
                .queryParam("QueryType", queryType)
                .queryParam("SearchTarget", searchTarget)
                .queryParam("MaxResults", maxResults);

        if (categoryId != null && !categoryId.isBlank()) {
            uriBuilder.queryParam("CategoryId", categoryId);
        }

        return execute(uriBuilder.build().toUri()).stream()
                .map(AladinConverter::toBookPreviewDto)
                .toList();
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

        return AladinConverter.toBookDetailDto(execute(uri).get(0));
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
            return Collections.emptyList();
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
}
