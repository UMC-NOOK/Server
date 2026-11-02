package app.nook.yes24.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.config.CacheConfig;
import app.nook.global.exception.CustomException;
import app.nook.yes24.converter.Yes24Converter;
import app.nook.yes24.dto.Yes24ResponseDto;
import app.nook.yes24.exception.Yes24ErrorCode;
import app.nook.yes24.utils.Yes24Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class Yes24Service {

    public static final int SEARCH_PAGE_SIZE = 100; // YES24 itemList 최대값 (초과 시 400)
    private static final int BESTSELLER_MAX_PAGE_SIZE = 100;

    private static final String SEARCH_PATH = "/v1/goods/itemList";
    private static final String DETAIL_PATH = "/v1/goods/itemDetail";
    private static final String BESTSELLER_PATH = "/v1/category/bestseller";
    private static final String INVALID_QUERY_ERROR_CODE = "PARAM_007";

    private final RestClient restClient;

    // 검색 원본 페이지 조회 (국내 종이책, 대표작·인기 도서 우선인 DEFAULT 정렬). 원본 페이지 단위로 캐시
    @Cacheable(
            cacheNames = CacheConfig.BOOK_SEARCH_PAGE_CACHE,
            key = "#keyword + ':' + #page",
            sync = true
    )
    public Yes24ResponseDto.SearchPage fetchSearchPage(String keyword, int page) {
        String query = toSearchQuery(keyword);
        if (query.isEmpty()) {
            return Yes24ResponseDto.SearchPage.empty();
        }
        Optional<Yes24ResponseDto.Data> data = execute(builder -> builder.path(SEARCH_PATH)
                .queryParam("query", "{query}")
                .queryParam("category", "BOOK")
                .queryParam("sort", "DEFAULT")
                .queryParam("page", page)
                .queryParam("pageSize", SEARCH_PAGE_SIZE)
                .build(Map.of("query", query)));

        return data
                .map(d -> new Yes24ResponseDto.SearchPage(
                        itemsOf(d), d.getTotalCount() != null ? d.getTotalCount() : 0L))
                .orElseGet(Yes24ResponseDto.SearchPage::empty);
    }

    // 도서 상세 정보 조회
    public BookResponseDto.BookDetailDto lookupItem(String isbn13) {
        List<Yes24ResponseDto.Yes24Item> items = execute(builder -> builder.path(DETAIL_PATH)
                .queryParam("searchType", "ISBN13")
                .queryParam("query", isbn13)
                .queryParam("detail", "Y")
                .build())
                .map(this::itemsOf)
                .orElse(List.of());

        if (items.isEmpty()) {
            throw new CustomException(BookErrorCode.ISBN13_NOT_FOUND);
        }

        Yes24ResponseDto.Yes24Item item = items.get(0);
        if (!Yes24Utils.isValid(item)) {
            log.info("[LOOKUP_INVALID] isbn={}, goodsSortNm='{}', adultYn={}",
                    isbn13, item.getGoodsSortNm(), item.getAdultYn());
            throw new CustomException(BookErrorCode.BOOK_NOT_ALLOWED);
        }

        String categoryName = Yes24Utils.resolveServiceCategory(item.getGoodsSortNm()).orElseThrow();
        return Yes24Converter.toBookDetailDto(item, categoryName);
    }

    // 카테고리 베스트셀러에서 유효한 도서만 targetCount개
    public List<BookResponseDto.BookPreviewDto> fetchBestsellers(String categoryId, int targetCount) {
        // 필터로 빠지는 상품을 감안해 4배수로 요청
        int pageSize = Math.min(targetCount * 4, BESTSELLER_MAX_PAGE_SIZE);
        List<Yes24ResponseDto.Yes24Item> rawItems = execute(builder -> builder.path(BESTSELLER_PATH)
                .queryParam("categoryId", categoryId)
                .queryParam("page", 1)
                .queryParam("pageSize", pageSize)
                .build())
                .map(this::itemsOf)
                .orElse(List.of());

        List<BookResponseDto.BookPreviewDto> result = new ArrayList<>();
        for (Yes24ResponseDto.Yes24Item item : rawItems) {
            if (!Yes24Utils.isValid(item)) {
                continue;
            }
            result.add(Yes24Converter.toBookPreviewDto(item, result.size() + 1));
            if (result.size() >= targetCount) {
                break;
            }
        }

        if (result.size() < targetCount) {
            log.warn("[FETCH_WARNING] reason='Insufficient count', categoryId={}, target={}, fetched={}",
                    categoryId, targetCount, result.size());
        }
        return result;
    }

    // 404는 "결과 없음"으로 보고 empty, 그 외 오류는 공급자 중립 에러 코드로 변환
    private Optional<Yes24ResponseDto.Data> execute(Function<UriBuilder, URI> uriFunction) {
        try {
            Yes24ResponseDto.Yes24ApiResult response = restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(Yes24ResponseDto.Yes24ApiResult.class);

            if (response == null || !response.isSuccess()) {
                log.error("[YES24_API_FAIL] errorCode={}, message='{}'",
                        response == null ? null : response.getErrorCode(),
                        response == null ? null : response.getMessage());
                throw new CustomException(Yes24ErrorCode.BOOK_API_ERROR);
            }
            return Optional.ofNullable(response.getData());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.debug("[YES24_NOT_FOUND] body={}", e.getResponseBodyAsString());
                return Optional.empty();
            }
            // 허용되지 않는 검색어 패턴(예: ' OR 1=1)은 결과 없음으로 처리
            if (e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)
                    && e.getResponseBodyAsString().contains(INVALID_QUERY_ERROR_CODE)) {
                log.warn("[YES24_INVALID_QUERY] body={}", e.getResponseBodyAsString());
                return Optional.empty();
            }
            log.error("[YES24_API_ERROR] status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                throw new CustomException(Yes24ErrorCode.BOOK_API_RATE_LIMITED);
            }
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
                    || e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
                throw new CustomException(Yes24ErrorCode.BOOK_API_UNAUTHORIZED);
            }
            throw new CustomException(Yes24ErrorCode.BOOK_API_ERROR);

        } catch (RestClientException e) {
            log.error("[YES24_API_ERROR] message='{}'", e.getMessage());
            throw new CustomException(Yes24ErrorCode.BOOK_API_ERROR);
        }
    }

    // YES24는 검색어에 <, >, -- 가 있으면 400(PARAM_007)을 주므로 공백으로 바꾼다
    static String toSearchQuery(String keyword) {
        return keyword.replaceAll("[<>]", " ")
                .replaceAll("-{2,}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<Yes24ResponseDto.Yes24Item> itemsOf(Yes24ResponseDto.Data data) {
        return data.getItems() != null ? data.getItems() : Collections.emptyList();
    }
}
