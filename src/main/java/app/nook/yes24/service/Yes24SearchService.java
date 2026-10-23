package app.nook.yes24.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.yes24.converter.Yes24Converter;
import app.nook.yes24.dto.Yes24ResponseDto;
import app.nook.yes24.utils.Yes24Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 커서 기반 무한스크롤 검색.
 * 커서는 YES24 원본 결과 전체에서의 위치(Global Index)다. (0~99: 1페이지, 100~199: 2페이지 ...)
 * 필터로 빠지는 항목이 있어 size가 찰 때까지 다음 원본 페이지를 이어서 읽는다.
 * 원본 페이지는 Yes24Service에서 페이지 단위로 캐시되므로 같은 페이지 안의 스크롤은 다시 호출하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Yes24SearchService {

    private static final int PAGE_SIZE = Yes24Service.SEARCH_PAGE_SIZE;
    private static final int MAX_PAGES_PER_REQUEST = 3; // 한 요청이 YES24를 여러 번 부르지 않도록 상한

    private final Yes24Service yes24Service;

    public BookResponseDto.SearchResultDto searchItems(String keyword, Integer cursor, int size) {
        List<BookResponseDto.BookSearchDto> results = new ArrayList<>();
        long totalResults = 0L;
        int currentGlobalIndex = (cursor == null) ? 0 : cursor;
        boolean lastPageReached = false;
        int pageFetchCount = 0;

        while (results.size() < size && pageFetchCount < MAX_PAGES_PER_REQUEST) {
            int page = currentGlobalIndex / PAGE_SIZE + 1;
            int startIndex = currentGlobalIndex % PAGE_SIZE;

            Yes24ResponseDto.SearchPage searchPage = yes24Service.fetchSearchPage(keyword, page);
            pageFetchCount++;
            totalResults = searchPage.totalCount();
            List<Yes24ResponseDto.Yes24Item> rawItems = searchPage.items();

            int filteredCount = 0;
            for (int i = startIndex; i < rawItems.size() && results.size() < size; i++) {
                currentGlobalIndex++;
                Yes24ResponseDto.Yes24Item item = rawItems.get(i);
                if (!Yes24Utils.isValid(item)) {
                    filteredCount++;
                    continue;
                }
                results.add(Yes24Converter.toBookSearchDto(item));
            }
            if (filteredCount > 0) {
                log.debug("[FILTERING] page={}, filteredCount={}", page, filteredCount);
            }

            // 원본 페이지가 덜 찼으면 마지막 페이지. 끝까지 읽었으면 더 가져올 것이 없다
            if (rawItems.size() < PAGE_SIZE) {
                int pageStart = (page - 1) * PAGE_SIZE;
                lastPageReached = currentGlobalIndex >= pageStart + rawItems.size();
                break;
            }
        }

        if (pageFetchCount >= MAX_PAGES_PER_REQUEST && results.size() < size) {
            log.warn("[MAX_LIMIT_REACHED] keyword='{}', limit={}, fetched={}",
                    keyword, MAX_PAGES_PER_REQUEST, results.size());
        }

        boolean hasNext = !results.isEmpty() && !lastPageReached && currentGlobalIndex < totalResults;
        return new BookResponseDto.SearchResultDto(
                totalResults,
                hasNext,
                hasNext ? currentGlobalIndex : null,
                results
        );
    }
}
