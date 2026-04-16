package app.nook.controller.book;

import app.nook.book.domain.enums.SearchType;
import app.nook.book.controller.BookSearchController;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.dto.LibrarySearchHomeResponseDto;
import app.nook.book.facade.BookSearchFacade;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookSearchController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class BookSearchControllerTest extends AbstractWebMvcRestDocsTests {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_KEYWORD = "채식주의자";
    private static final String TEST_ISBN_1 = "9788936434267";
    private static final String TEST_ISBN_2 = "9788936433598";

    @MockitoBean
    private BookSearchFacade bookSearchFacade;

    @Test
    @WithCustomUser
    @DisplayName("내 서재 검색 홈 조회 성공")
    void 내서재검색홈_조회_성공() throws Exception {
        LibrarySearchHomeResponseDto.Result response = new LibrarySearchHomeResponseDto.Result(List.of(
                LibrarySearchHomeResponseDto.RecentFocusSection.of(List.of(
                        new LibrarySearchHomeResponseDto.RecentFocusItem(1L, "포커스 책", "저자1", "https://example.com/focus.jpg")
                )),
                LibrarySearchHomeResponseDto.BeforeReadingSection.of(List.of(
                        new LibrarySearchHomeResponseDto.BeforeReadingItem(2L, "읽기 전 책", "저자2", "https://example.com/before.jpg")
                ))
        ));

        given(bookSearchFacade.getLibrarySearchHome(any())).willReturn(response);

        mockMvc.perform(
                        get("/api/v1/books/search/library/home")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sections[0].type").value("RECENT_FOCUS"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        librarySearchHomeResponseFields()
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("내 서재 검색 홈 조회 성공 - 읽기 전만")
    void 내서재검색홈_조회_읽기전만_성공() throws Exception {
        LibrarySearchHomeResponseDto.Result response = new LibrarySearchHomeResponseDto.Result(List.of(
                LibrarySearchHomeResponseDto.BeforeReadingSection.of(List.of(
                        new LibrarySearchHomeResponseDto.BeforeReadingItem(2L, "읽기 전 책", "저자2", "https://example.com/before.jpg")
                ))
        ));

        given(bookSearchFacade.getLibrarySearchHome(any())).willReturn(response);

        mockMvc.perform(
                        get("/api/v1/books/search/library/home")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sections[0].type").value("BEFORE_READING"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        librarySearchHomeResponseFields()
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("내 서재 검색 홈 조회 성공 - 최근 포커스만")
    void 내서재검색홈_조회_최근포커스만_성공() throws Exception {
        LibrarySearchHomeResponseDto.Result response = new LibrarySearchHomeResponseDto.Result(List.of(
                LibrarySearchHomeResponseDto.RecentFocusSection.of(List.of(
                        new LibrarySearchHomeResponseDto.RecentFocusItem(1L, "포커스 책", "저자1", "https://example.com/focus.jpg")
                ))
        ));

        given(bookSearchFacade.getLibrarySearchHome(any())).willReturn(response);

        mockMvc.perform(
                        get("/api/v1/books/search/library/home")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sections[0].type").value("RECENT_FOCUS"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        librarySearchHomeResponseFields()
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("내 서재 검색 홈 조회 성공 - 추천만")
    void 내서재검색홈_조회_추천만_성공() throws Exception {
        LibrarySearchHomeResponseDto.Result response = new LibrarySearchHomeResponseDto.Result(List.of(
                LibrarySearchHomeResponseDto.RecommendationSection.of(List.of(
                        new LibrarySearchHomeResponseDto.RecommendationItem(TEST_ISBN_1, "추천 책", "저자3", "https://example.com/recommend.jpg")
                ))
        ));

        given(bookSearchFacade.getLibrarySearchHome(any())).willReturn(response);

        mockMvc.perform(
                        get("/api/v1/books/search/library/home")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sections[0].type").value("RECOMMENDATION"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        librarySearchHomeResponseFields()
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 성공 - 첫 검색 (cursor=null)")
    void 도서검색_첫검색_성공() throws Exception {
        // given
        BookResponseDto.SearchResultDto response = createSearchResult(2, true, 2);

        given(bookSearchFacade.searchBooks(TEST_USER_ID, TEST_KEYWORD, null, SearchType.GLOBAL))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(2))
                .andExpect(jsonPath("$.result.books").isArray())
                .andExpect(jsonPath("$.result.books.length()").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.nextCursor").value(2))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("type").description("검색 타입 (GLOBAL: 전체 도서, LIBRARY: 내 서재)")
                        ),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드"),
                                parameterWithName("cursor").description("커서 (첫 검색 시 생략, 페이지네이션 시 이전 응답의 nextCursor 값)").optional()
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.totalResults").description("전체 검색 결과 수"),
                                        fieldWithPath("result.hasNext").description("다음 페이지 존재 여부"),
                                        fieldWithPath("result.nextCursor").description("다음 페이지 커서 (없으면 null)").optional(),
                                        fieldWithPath("result.books").description("검색된 도서 목록"),
                                        fieldWithPath("result.books[].isbn13").description("ISBN13"),
                                        fieldWithPath("result.books[].title").description("도서 제목"),
                                        fieldWithPath("result.books[].mallType").description("몰 타입(국내 도서, 전자책)"),
                                        fieldWithPath("result.books[].author").description("저자"),
                                        fieldWithPath("result.books[].coverImageUrl").description("표지 이미지 URL"),
                                        fieldWithPath("result.books[].publisher").description("출판사"),
                                        fieldWithPath("result.books[].publicationDate").description("출판일"),
                                        fieldWithPath("result.books[].inLibrary").description("내 서재 등록 여부"),
                                        fieldWithPath("result.books[].readingStatus").description("내 서재 독서 상태").optional()
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 성공 - 페이지네이션 (cursor 사용)")
    void 도서검색_페이지네이션_성공() throws Exception {
        // given
        Integer cursor = 2; // 2번째 책부터
        BookResponseDto.SearchResultDto response = createSearchResult(2, true, 4);

        given(bookSearchFacade.searchBooks(TEST_USER_ID, TEST_KEYWORD, cursor, SearchType.GLOBAL))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .param("cursor", String.valueOf(cursor))
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nextCursor").value(4));
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 실패 - 빈 검색어")
    void 도서검색_빈검색어_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "GLOBAL")
                                .param("keyword", "") // 빈 문자열
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 실패 - 검색어 누락")
    void 도서검색_검색어누락_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "GLOBAL")
                                // keyword 파라미터 없음
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 실패 - 음수 커서")
    void 도서검색_음수커서_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .param("cursor", "-1") // 음수
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("도서 검색 실패 - 잘못된 검색 타입")
    void 도서검색_잘못된타입_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}", "INVALID_TYPE")
                                .param("keyword", TEST_KEYWORD)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest()); // TypeMismatchException 발생
    }

    @Test
    @WithCustomUser
    @DisplayName("검색 기록 조회 성공")
    void 검색기록조회_성공() throws Exception {
        // given
        List<String> histories = Arrays.asList("채식주의자", "소년이 온다", "한강");

        given(bookSearchFacade.getSearchHistories(TEST_USER_ID, SearchType.GLOBAL))
                .willReturn(histories);

        // when & then
        mockMvc.perform(
                        get("/api/v1/books/search/{type}/histories", "GLOBAL")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result.length()").value(3))
                .andExpect(jsonPath("$.result[0]").value("채식주의자"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("type").description("검색 타입 (GLOBAL: 전체 도서, LIBRARY: 내 서재)")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result[]").description("최근 검색어 목록 (최신순)")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("특정 검색어 삭제 성공")
    void 검색기록삭제_성공() throws Exception {
        // given
        doNothing().when(bookSearchFacade)
                .deleteSearchHistory(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);

        // when & then
        mockMvc.perform(
                        delete("/api/v1/books/search/{type}/histories?keyword={keyword}", "GLOBAL", TEST_KEYWORD)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("type").description("검색 타입 (GLOBAL: 전체 도서, LIBRARY: 내 서재)")
                        ),
                        queryParameters(
                                parameterWithName("keyword").description("삭제할 검색어")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("검색어 삭제 실패 - 검색어 누락")
    void 검색기록삭제_검색어누락_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/api/v1/books/search/{type}/histories", "GLOBAL")
                                // keyword 파라미터 없음
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("전체 검색 기록 삭제 성공")
    void 검색기록전체삭제_성공() throws Exception {
        // given
        doNothing().when(bookSearchFacade)
                .deleteAllSearchHistories(TEST_USER_ID, SearchType.GLOBAL);

        // when & then
        mockMvc.perform(
                        delete("/api/v1/books/search/{type}/histories/all", "GLOBAL")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("type").description("검색 타입 (GLOBAL: 전체 도서, LIBRARY: 내 서재)")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ));
    }

    // === 헬퍼 메서드 ===

    private BookResponseDto.SearchResultDto createSearchResult(
            int bookCount, boolean hasNext, Integer nextCursor) {

        List<BookResponseDto.BookSearchDto> books = bookCount > 0
                ? createMockBooks(bookCount)
                : Collections.emptyList();

        return new BookResponseDto.SearchResultDto(
                (long) bookCount,
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
                    .mallType("도서")
                    .author("한강")
                    .publisher("창비")
                    .publicationDate("2024-01-01")
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .inLibrary(false)
                    .build());
        }
        return books;
    }

    private static ResponseFieldsSnippet librarySearchHomeResponseFields() {
        return responseFields(
                ApiResponseSnippet.withResult(
                        fieldWithPath("result.sections").type(ARRAY).description("검색 홈 섹션 목록"),
                        fieldWithPath("result.sections[].type").type(STRING).description("섹션 타입 (RECENT_FOCUS, BEFORE_READING, RECOMMENDATION)"),
                        fieldWithPath("result.sections[].title").type(STRING).description("섹션 제목"),
                        fieldWithPath("result.sections[].items").type(ARRAY).description("섹션별 도서 아이템 목록"),
                        fieldWithPath("result.sections[].items[].bookId").type(NUMBER).optional().description("서재 도서 ID (최근 포커스/읽기 전 섹션)"),
                        fieldWithPath("result.sections[].items[].isbn13").type(STRING).optional().description("추천 도서 ISBN13 (추천 섹션)"),
                        fieldWithPath("result.sections[].items[].title").type(STRING).description("도서 제목"),
                        fieldWithPath("result.sections[].items[].author").type(STRING).description("도서 저자"),
                        fieldWithPath("result.sections[].items[].coverUrl").type(STRING).description("도서 커버 URL")
                )
        );
    }
}
