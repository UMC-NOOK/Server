package app.nook.book.facade;

import app.nook.aladin.service.AladinService;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.dto.LibrarySearchHomeResponseDto;
import app.nook.book.service.BookService;
import app.nook.book.service.SearchHistoryService;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSearchFacadeTest {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_KEYWORD = "채식주의자";
    private static final String TEST_ISBN_1 = "9788936434267";
    private static final String TEST_ISBN_2 = "9788936433598";
    private static final int DEFAULT_PAGE_SIZE = 10; // 한 번에 가져올 책 개수

    @Mock
    private AladinService aladinService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @Mock
    private LibraryService libraryService;

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookSearchFacade bookSearchFacade;

    @Test
    @DisplayName("전체 도서 검색 성공 - 첫 검색 시 이력 저장")
    void searchBooks_첫검색_이력저장() {
        // given
        Integer cursor = null; // 첫 검색 (0번째 책부터)
        BookResponseDto.SearchResultDto mockResult = createSearchResult(
                100L,
                10,    // 0~9번째 책 10권 반환
                true,  // 다음 페이지 있음
                10     // nextCursor = 10 (다음엔 10번째 책부터)
        );

        given(aladinService.searchItems(TEST_KEYWORD, cursor, DEFAULT_PAGE_SIZE))
                .willReturn(mockResult);
        given(libraryService.findOwnedIsbns(eq(TEST_USER_ID), anyList()))
                .willReturn(Collections.emptySet()); // 서재에 없는 경우

        // when
        BookResponseDto.SearchResultDto result = bookSearchFacade.searchBooks(
                TEST_USER_ID, TEST_KEYWORD, cursor, SearchType.GLOBAL);

        // then
        assertThat(result.totalResults()).isEqualTo(100L);
        assertThat(result.books()).hasSize(10);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(10);

        // 첫 검색이므로 검색 이력 저장됨
        verify(searchHistoryService, times(1))
                .saveKeyword(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);
        verify(aladinService, times(1))
                .searchItems(TEST_KEYWORD, cursor, DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("전체 도서 검색 - 페이지네이션 시 이력 저장 안 됨")
    void searchBooks_페이지네이션_이력저장안됨() {
        // given
        Integer cursor = 10; // 10번째 책부터 (두 번째 페이지)
        BookResponseDto.SearchResultDto mockResult = createSearchResult(
                100L,
                10,    // 10~19번째 책 10권 반환
                true,  // 다음 페이지 있음
                20     // nextCursor = 20 (다음엔 20번째 책부터)
        );

        given(aladinService.searchItems(TEST_KEYWORD, cursor, DEFAULT_PAGE_SIZE))
                .willReturn(mockResult);
        given(libraryService.findOwnedIsbns(eq(TEST_USER_ID), anyList()))
                .willReturn(Collections.emptySet()); // 서재에 없는 경우

        // when
        BookResponseDto.SearchResultDto result = bookSearchFacade.searchBooks(
                TEST_USER_ID, TEST_KEYWORD, cursor, SearchType.GLOBAL);

        // then
        assertThat(result.books()).hasSize(10);
        assertThat(result.nextCursor()).isEqualTo(20);

        // 페이지네이션이므로 검색 이력 저장 안 됨
        verify(searchHistoryService, never()).saveKeyword(eq(TEST_USER_ID), eq(TEST_KEYWORD), any());
        verify(aladinService, times(1))
                .searchItems(TEST_KEYWORD, cursor, DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("전체 도서 검색 - 검색 결과 없음")
    void searchBooks_결과없음() {
        // given
        Integer cursor = null;
        BookResponseDto.SearchResultDto emptyResult = createSearchResult(
                0L,
                0,     // 책 0권
                false, // 다음 페이지 없음
                null   // nextCursor = null
        );

        given(aladinService.searchItems(TEST_KEYWORD, cursor, DEFAULT_PAGE_SIZE))
                .willReturn(emptyResult);

        // when
        BookResponseDto.SearchResultDto result = bookSearchFacade.searchBooks(
                TEST_USER_ID, TEST_KEYWORD, cursor, SearchType.GLOBAL);

        // then
        assertThat(result.totalResults()).isEqualTo(0L);
        assertThat(result.books()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();

        // 결과가 없어도 첫 검색이므로 검색 이력은 저장됨
        verify(searchHistoryService, times(1))
                .saveKeyword(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);
    }

//    @Test
//    @DisplayName("서재 내 검색")
//    void searchBooks_서재검색() {
//    }

    @Test
    @DisplayName("검색 이력 조회 성공")
    void getSearchHistories_성공() {
        // given
        List<String> mockHistories = Arrays.asList("채식주의자", "소년이 온다", "한강");

        given(searchHistoryService.getSearchHistories(TEST_USER_ID, SearchType.GLOBAL))
                .willReturn(mockHistories);

        // when
        List<String> result = bookSearchFacade.getSearchHistories(TEST_USER_ID, SearchType.GLOBAL);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("채식주의자");
        assertThat(result.get(1)).isEqualTo("소년이 온다");

        verify(searchHistoryService, times(1))
                .getSearchHistories(TEST_USER_ID, SearchType.GLOBAL);
    }

    @Test
    @DisplayName("특정 검색어 삭제 성공")
    void deleteSearchHistory_성공() {
        // given
        doNothing().when(searchHistoryService)
                .deleteHistory(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);

        // when
        bookSearchFacade.deleteSearchHistory(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);

        // then
        verify(searchHistoryService, times(1))
                .deleteHistory(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);
    }

    @Test
    @DisplayName("전체 검색 이력 삭제 성공")
    void deleteAllSearchHistories_성공() {
        // given
        doNothing().when(searchHistoryService)
                .deleteAllHistories(TEST_USER_ID, SearchType.GLOBAL);

        // when
        bookSearchFacade.deleteAllSearchHistories(TEST_USER_ID, SearchType.GLOBAL);

        // then
        verify(searchHistoryService, times(1))
                .deleteAllHistories(TEST_USER_ID, SearchType.GLOBAL);
    }

    @Nested
    @DisplayName("내 서재 검색 홈 조회")
    class GetLibrarySearchHome {

        @Test
        @DisplayName("최근 포커스와 읽기 전이 모두 있으면 해당 순서대로 섹션을 반환한다")
        void 최근포커스와_읽기전이_모두있음() {
            User user = user();

            given(libraryService.viewRecentFocusBooks(eq(user), anyInt()))
                    .willReturn(List.of(
                            new LibraryViewDto.RecentFocusBookItem(10L, "포커스 책", "작가A", "focus.jpg")
                    ));
            given(libraryService.viewBeforeReadingBooks(eq(user)))
                    .willReturn(new LibraryViewDto.BeforeReadingResponseDto(List.of(
                            new LibraryViewDto.BeforeBookItem(20L, "읽기 전 책", "작가B", "before.jpg")
                    )));

            LibrarySearchHomeResponseDto.Result result = bookSearchFacade.getLibrarySearchHome(user);

            assertThat(result.sections()).hasSize(2);
            assertThat(result.sections().get(0)).isInstanceOf(LibrarySearchHomeResponseDto.RecentFocusSection.class);
            assertThat(result.sections().get(1)).isInstanceOf(LibrarySearchHomeResponseDto.BeforeReadingSection.class);
            verify(bookService, never()).getPersonalizedBestsellers(any());
        }

        @Test
        @DisplayName("최근 포커스만 있으면 최근 포커스 섹션만 반환한다")
        void 최근포커스만_있음() {
            User user = user();

            given(libraryService.viewRecentFocusBooks(eq(user), anyInt()))
                    .willReturn(List.of(
                            new LibraryViewDto.RecentFocusBookItem(10L, "포커스 책", "작가A", "focus.jpg")
                    ));
            given(libraryService.viewBeforeReadingBooks(eq(user)))
                    .willReturn(new LibraryViewDto.BeforeReadingResponseDto(List.of()));

            LibrarySearchHomeResponseDto.Result result = bookSearchFacade.getLibrarySearchHome(user);

            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0)).isInstanceOf(LibrarySearchHomeResponseDto.RecentFocusSection.class);
            verify(bookService, never()).getPersonalizedBestsellers(any());
        }

        @Test
        @DisplayName("읽기 전만 있으면 읽기 전 섹션만 반환한다")
        void 읽기전만_있음() {
            User user = user();

            given(libraryService.viewRecentFocusBooks(eq(user), anyInt()))
                    .willReturn(List.of());
            given(libraryService.viewBeforeReadingBooks(eq(user)))
                    .willReturn(new LibraryViewDto.BeforeReadingResponseDto(List.of(
                            new LibraryViewDto.BeforeBookItem(20L, "읽기 전 책", "작가B", "before.jpg")
                    )));

            LibrarySearchHomeResponseDto.Result result = bookSearchFacade.getLibrarySearchHome(user);

            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0)).isInstanceOf(LibrarySearchHomeResponseDto.BeforeReadingSection.class);
            verify(bookService, never()).getPersonalizedBestsellers(any());
        }

        @Test
        @DisplayName("최근 포커스와 읽기 전이 모두 없으면 추천 섹션만 반환한다")
        void 둘다없으면_추천만_반환() {
            User user = user();

            given(libraryService.viewRecentFocusBooks(eq(user), anyInt()))
                    .willReturn(List.of());
            given(libraryService.viewBeforeReadingBooks(eq(user)))
                    .willReturn(new LibraryViewDto.BeforeReadingResponseDto(List.of()));
            given(bookService.getPersonalizedBestsellers(eq(user)))
                    .willReturn(List.of(
                            new BookResponseDto.BookPreviewDto("9780000000001", "추천 책1", "작가C", "recommend1.jpg", "출판사", 1),
                            new BookResponseDto.BookPreviewDto("9780000000002", "추천 책2", "작가C", "recommend2.jpg", "출판사", 1),
                            new BookResponseDto.BookPreviewDto("9780000000003", "추천 책3", "작가C", "recommend3.jpg", "출판사", 1),
                            new BookResponseDto.BookPreviewDto("9780000000004", "추천 책4", "작가C", "recommend4.jpg", "출판사", 1),
                            new BookResponseDto.BookPreviewDto("9780000000005", "추천 책5", "작가C", "recommend5.jpg", "출판사", 1),
                            new BookResponseDto.BookPreviewDto("9780000000006", "추천 책6", "작가C", "recommend6.jpg", "출판사", 1)
                    ));

            LibrarySearchHomeResponseDto.Result result = bookSearchFacade.getLibrarySearchHome(user);

            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0)).isInstanceOf(LibrarySearchHomeResponseDto.RecommendationSection.class);
            LibrarySearchHomeResponseDto.RecommendationSection section =
                    (LibrarySearchHomeResponseDto.RecommendationSection) result.sections().get(0);
            assertThat(section.items()).hasSize(5);
            assertThat(section.items()).extracting(LibrarySearchHomeResponseDto.RecommendationItem::isbn13)
                    .doesNotContain("9780000000006");
            verify(bookService).getPersonalizedBestsellers(user);
        }
    }

    // === 헬퍼 메서드 ===

    private User user() {
        return User.builder()
                .email("user@test.com")
                .nickName("user")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
    }

    /**
     * 검색 결과 더미 데이터 생성
     * @param bookCount 반환할 책 개수
     * @param hasNext 다음 페이지 존재 여부
     * @param nextCursor 다음 검색 시작 책 번호
     */
    private BookResponseDto.SearchResultDto createSearchResult(
            long totalResults, int bookCount, boolean hasNext, Integer nextCursor) {

        List<BookResponseDto.BookSearchDto> books = bookCount > 0
                ? createMockBooks(bookCount)
                : Collections.emptyList();

        return new BookResponseDto.SearchResultDto(
                totalResults,
                hasNext,
                nextCursor,
                books
        );
    }

    private List<BookResponseDto.BookSearchDto> createMockBooks(int count) {
        List<BookResponseDto.BookSearchDto> books = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            books.add(BookResponseDto.BookSearchDto.builder()
                    .isbn13(i == 0 ? TEST_ISBN_1 : TEST_ISBN_2)
                    .title("테스트 도서 " + (i + 1))
                    .author("한강")
                    .publisher("창비")
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .build());
        }
        return books;
    }
}
