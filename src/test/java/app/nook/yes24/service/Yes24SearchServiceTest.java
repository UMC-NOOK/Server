package app.nook.yes24.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.yes24.dto.Yes24ResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Yes24SearchServiceTest {

    private static final String KEYWORD = "한강";
    private static final int SIZE = 10;

    @Mock
    private Yes24Service yes24Service;

    @InjectMocks
    private Yes24SearchService yes24SearchService;

    @Test
    @DisplayName("첫 검색 - 1페이지에서 size만큼 반환하고 다음 커서는 읽은 위치")
    void searchItems_firstPage() {
        given(yes24Service.fetchSearchPage(KEYWORD, 1)).willReturn(page(0, 100, 1000));

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, null, SIZE);

        assertThat(result.books()).hasSize(10);
        assertThat(result.books().get(0).getIsbn13()).isEqualTo(isbn(0));
        assertThat(result.totalResults()).isEqualTo(1000L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(10);
    }

    @Test
    @DisplayName("스크롤 - 커서가 같은 원본 페이지 안이면 그 페이지의 다음 항목부터")
    void searchItems_cursorInSamePage() {
        given(yes24Service.fetchSearchPage(KEYWORD, 1)).willReturn(page(0, 100, 1000));

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, 10, SIZE);

        assertThat(result.books().get(0).getIsbn13()).isEqualTo(isbn(10));
        assertThat(result.nextCursor()).isEqualTo(20);
    }

    @Test
    @DisplayName("필터로 빠진 항목은 건너뛰고 다음 원본 페이지까지 이어서 채운다")
    void searchItems_skipsInvalidAndContinuesToNextPage() {
        List<Yes24ResponseDto.Yes24Item> firstPage = new ArrayList<>(page(0, 100, 1000).items());
        firstPage.set(96, invalidItem());
        firstPage.set(97, invalidItem());
        given(yes24Service.fetchSearchPage(KEYWORD, 1))
                .willReturn(new Yes24ResponseDto.SearchPage(firstPage, 1000));
        given(yes24Service.fetchSearchPage(KEYWORD, 2)).willReturn(page(100, 100, 1000));

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, 95, SIZE);

        // 95, 98, 99 + 다음 페이지 100~106
        assertThat(result.books()).extracting(BookResponseDto.BookSearchDto::getIsbn13)
                .containsExactly(isbn(95), isbn(98), isbn(99), isbn(100), isbn(101), isbn(102),
                        isbn(103), isbn(104), isbn(105), isbn(106));
        assertThat(result.nextCursor()).isEqualTo(107);
    }

    @Test
    @DisplayName("마지막 원본 페이지를 끝까지 읽으면 다음 페이지 없음")
    void searchItems_lastPage() {
        given(yes24Service.fetchSearchPage(KEYWORD, 1)).willReturn(page(0, 3, 3));

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, null, SIZE);

        assertThat(result.books()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("검색 결과 없음 - 빈 결과, 다음 페이지 없음")
    void searchItems_empty() {
        given(yes24Service.fetchSearchPage(KEYWORD, 1)).willReturn(Yes24ResponseDto.SearchPage.empty());

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, null, SIZE);

        assertThat(result.books()).isEmpty();
        assertThat(result.totalResults()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("한 요청에서 원본 페이지는 최대 3번까지만 조회")
    void searchItems_maxPagesPerRequest() {
        List<Yes24ResponseDto.Yes24Item> allInvalid = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            allInvalid.add(invalidItem());
        }
        given(yes24Service.fetchSearchPage(eq(KEYWORD), anyInt()))
                .willReturn(new Yes24ResponseDto.SearchPage(allInvalid, 1000));

        BookResponseDto.SearchResultDto result = yes24SearchService.searchItems(KEYWORD, null, SIZE);

        assertThat(result.books()).isEmpty();
        verify(yes24Service, times(3)).fetchSearchPage(eq(KEYWORD), anyInt());
    }

    // globalStart부터 count개의 유효한 국내도서 항목
    private Yes24ResponseDto.SearchPage page(int globalStart, int count, long totalCount) {
        List<Yes24ResponseDto.Yes24Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(Yes24ResponseDto.Yes24Item.builder()
                    .isbn13(isbn(globalStart + i))
                    .title("도서 " + (globalStart + i))
                    .adultYn("N")
                    .goodsSortNm("국내도서-소설/시/희곡")
                    .publishDate("20260101")
                    .build());
        }
        return new Yes24ResponseDto.SearchPage(items, totalCount);
    }

    private Yes24ResponseDto.Yes24Item invalidItem() {
        return Yes24ResponseDto.Yes24Item.builder()
                .isbn13("")
                .adultYn("N")
                .goodsSortNm("국내도서-전집")
                .build();
    }

    private String isbn(int n) {
        return String.format("978%010d", n);
    }
}
