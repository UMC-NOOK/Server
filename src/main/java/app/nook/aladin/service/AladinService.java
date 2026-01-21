package app.nook.aladin.service;

import app.nook.aladin.converter.AladinConverter;
import app.nook.aladin.dto.AladinResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

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


    // redis 추가 예정
    // 주간 베스트셀러 조회
    public List<AladinResponseDto.BookPreviewDto> getWeeklyBestsellers() {
        log.info("[Aladin] 주간 베스트셀러 조회 요청");
        URI uri = getBaseUriBuilder(ALADIN_LIST_PATH)
                .queryParam("QueryType", "Bestseller")
                .queryParam("MaxResults", 10)
                .queryParam("SearchTarget", "Book")
                .build()
                .toUri();
        AladinResponseDto.AladinApiResult response = callApi(uri);

        if (isEmpty(response)) {
            log.warn("[Aladin] 주간 베스트셀러 응답이 비어있습니다.");
            return Collections.emptyList();
        }

        List<AladinResponseDto.BookPreviewDto> results = response.getItem().stream()
                .map(AladinConverter::toBookPreviewDto)
                .toList();

        log.info("[Aladin] 주간 베스트셀러 조회 완료 - 총 {}권", results.size());

        return results;
    }

    public List<AladinResponseDto.BookPreviewDto> getBestSellersByCategory(String categoryId) {
        log.info("[Aladin] 카테고리별 베스트셀러 조회 요청 - CategoryId={}", categoryId);
        URI uri = getBaseUriBuilder(ALADIN_LIST_PATH)
                .queryParam("QueryType", "Bestseller")
                .queryParam("MaxResults", 5)
                .queryParam("SearchTarget", "Book")
                .queryParam("CategoryId", categoryId)
                .build()
                .toUri();
        AladinResponseDto.AladinApiResult response = callApi(uri);

        if (isEmpty(response)) {
            log.warn("[Aladin] 카테고리별 베스트셀러 응답이 비어있습니다. - CategoryId={}", categoryId);
            return Collections.emptyList();
        }

        List<AladinResponseDto.BookPreviewDto> results = response.getItem().stream()
                .map(AladinConverter::toBookPreviewDto)
                .toList();
        log.info("[Aladin] 카테고리별 베스트셀러 조회 완료 - CategoryId={}, 총 {}권", categoryId, results.size());
        return results;
    }


    private AladinResponseDto.AladinApiResult callApi(URI uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(AladinResponseDto.AladinApiResult.class);
        } catch (Exception e) {
            log.error("Aladin API 호출 실패: URL={}, Error={}", uri, e.getMessage());
            return new AladinResponseDto.AladinApiResult(); // null 대신 빈 객체 반환
        }
    }

    private boolean isEmpty(AladinResponseDto.AladinApiResult response) {
        return response == null || response.getItem() == null;
    }
    // 공통 URL 빌더
    private UriComponentsBuilder getBaseUriBuilder(String path) {
        return UriComponentsBuilder.fromHttpUrl(ALADIN_BASE_URL + path)
                .queryParam("ttbkey", ttbKey)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101");
    }
}
