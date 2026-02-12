package app.nook.book.facade;

import app.nook.aladin.exception.AladinErrorCode;
import app.nook.aladin.service.AladinService;
import app.nook.book.converter.BookConverter;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.SearchErrorCode;
import app.nook.book.service.SearchHistoryService;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BookSearchFacade {

    private final AladinService aladinService;
    private final SearchHistoryService searchHistoryService;
    private final LibraryService libraryService;

    private static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * [Global] 전체 도서 검색 (알라딘 API + 서재 보유 여부 매핑)
     */
    @Transactional
    public BookResponseDto.SearchResultDto searchBooks(
            Long userId, String keyword, Integer cursor, SearchType searchType) {

        boolean isNewSearch = (cursor == null || cursor == 0);
        // 첫 페이지 검색 시에만 검색 기록 저장
        if (isNewSearch) {
            log.info("[SEARCH_REQUEST] userId={}, keyword='{}', type={}", userId, keyword, searchType);
            searchHistoryService.saveKeyword(userId, keyword, searchType);
        } else {
            log.debug("[SEARCH_SCROLL] userId={}, keyword={}, cursor={}", userId, keyword, cursor);
        }

        if (searchType == SearchType.GLOBAL) {
            return searchGlobalBooks(userId, keyword, cursor);
        } else if (searchType == SearchType.LIBRARY) {
            return searchLibraryBooks(userId, keyword, cursor);
        }
        log.error("[SEARCH_FAIL] error='Invalid SearchType', type={}", searchType);
        throw new CustomException(SearchErrorCode.INVALID_SEARCH_TYPE);
    }

    // 알라딘 API를 통한 전체 도서 검색
    private BookResponseDto.SearchResultDto searchGlobalBooks(Long userId, String keyword, Integer cursor) {
        BookResponseDto.SearchResultDto searchResult = aladinService.searchItems(keyword, cursor, DEFAULT_PAGE_SIZE);
        List<BookResponseDto.BookSearchDto> books = searchResult.books();

        int resultCount = searchResult.books().size();
        log.info("[SEARCH_RESULT] keyword='{}', foundCount={}, hasNext={}",
                keyword, resultCount, searchResult.hasNext());

        if (books.isEmpty()) {
            return searchResult;
        }

        // 도서 ISBN 리스트 추출
        List<String> isbns = books.stream()
                .map(BookResponseDto.BookSearchDto::getIsbn13)
                .toList();

        // 서재 보유 여부 조회
        Set<String> mybookIsbns = libraryService.findOwnedIsbns(userId, isbns);

        books.forEach(book -> {
            boolean isInLibrary = mybookIsbns.contains(book.getIsbn13());
            book.setInLibrary(isInLibrary);
        });

        return searchResult;
    }

    private BookResponseDto.SearchResultDto searchLibraryBooks(Long userId, String keyword, Integer cursor) {
        int offset = (cursor == null || cursor == 0) ? 0 : cursor;
        int page = offset / DEFAULT_PAGE_SIZE;

        Page<Library> result = libraryService.searchBooksInLibrary(userId, keyword, page, DEFAULT_PAGE_SIZE);
        List<BookResponseDto.BookSearchDto> books = result.getContent().stream()
                .map(BookConverter::toBookSearchDto)
                .toList();

        boolean hasNext = result.hasNext();
        Integer nextCursor = hasNext ? offset + books.size() : null;

        log.info("[LIBRARY_SEARCH] keyword='{}', foundCount={}, hasNext={}", keyword, books.size(), hasNext);

        return new BookResponseDto.SearchResultDto(
                result.getTotalElements(),
                hasNext,
                nextCursor,
                books
        );
    }

    /**
     * [History] 최근 검색어 조회
     */
    public List<String> getSearchHistories(Long userId, SearchType searchType) {
        return searchHistoryService.getSearchHistories(userId, searchType);
    }

    /**
     * [History] 특정 검색어 삭제
     */
    @Transactional
    public void deleteSearchHistory(Long userId, String keyword, SearchType searchType) {
        log.info("[HISTORY_DELETE] userId={}, keyword='{}', type={}", userId, keyword, searchType);
        searchHistoryService.deleteHistory(userId, keyword, searchType);
    }

    /**
     * [History] 전체 검색어 삭제
     */
    @Transactional
    public void deleteAllSearchHistories(Long userId, SearchType searchType) {
        log.info("[HISTORY_DELETE_ALL] userId={}, type={}", userId, searchType);
        searchHistoryService.deleteAllHistories(userId, searchType);
    }

}
