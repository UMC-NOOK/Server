package app.nook.controller.book;

import app.nook.book.domain.enums.SearchType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.facade.BookSearchFacade;
import app.nook.global.common.AbstractRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookSearchControllerTest extends AbstractRestDocsTests {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_KEYWORD = "채식주의자";
    private static final String TEST_ISBN_1 = "9788936434267";
    private static final String TEST_ISBN_2 = "9788936433598";

    @MockitoBean
    private BookSearchFacade bookSearchFacade;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUpUser() {
        User user = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", TEST_USER_ID);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("도서 검색 성공 - 첫 검색 (cursor=null)")
    void 도서검색_첫검색_성공() throws Exception {
        // given
        BookResponseDto.SearchResultDto response = createSearchResult(2, true, 2);

        given(bookSearchFacade.searchBooks(TEST_USER_ID, TEST_KEYWORD, null, SearchType.GLOBAL))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(100))
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
                                        fieldWithPath("result.books[].inLibrary").description("내 서재 등록 여부")
                                )
                        )
                ));
    }

    @Test
    @DisplayName("도서 검색 성공 - 페이지네이션 (cursor 사용)")
    void 도서검색_페이지네이션_성공() throws Exception {
        // given
        Integer cursor = 2; // 10번째 책부터
        BookResponseDto.SearchResultDto response = createSearchResult(2, true, 4);

        given(bookSearchFacade.searchBooks(TEST_USER_ID, TEST_KEYWORD, cursor, SearchType.GLOBAL))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .param("cursor", String.valueOf(cursor))
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nextCursor").value(4));
    }

    @Test
    @DisplayName("도서 검색 실패 - 빈 검색어")
    void 도서검색_빈검색어_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "GLOBAL")
                                .param("keyword", "") // 빈 문자열
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("도서 검색 실패 - 검색어 누락")
    void 도서검색_검색어누락_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "GLOBAL")
                                // keyword 파라미터 없음
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("도서 검색 실패 - 음수 커서")
    void 도서검색_음수커서_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "GLOBAL")
                                .param("keyword", TEST_KEYWORD)
                                .param("cursor", "-1") // 음수
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("도서 검색 실패 - 잘못된 검색 타입")
    void 도서검색_잘못된타입_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}", "INVALID_TYPE")
                                .param("keyword", TEST_KEYWORD)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest()); // TypeMismatchException 발생
    }

    @Test
    @DisplayName("검색 기록 조회 성공")
    void 검색기록조회_성공() throws Exception {
        // given
        List<String> histories = Arrays.asList("채식주의자", "소년이 온다", "한강");

        given(bookSearchFacade.getSearchHistories(TEST_USER_ID, SearchType.GLOBAL))
                .willReturn(histories);

        // when & then
        mockMvc.perform(
                        get("/api/books/search/{type}/histories", "GLOBAL")
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
    @DisplayName("특정 검색어 삭제 성공")
    void 검색기록삭제_성공() throws Exception {
        // given
        doNothing().when(bookSearchFacade)
                .deleteSearchHistory(TEST_USER_ID, TEST_KEYWORD, SearchType.GLOBAL);

        // when & then
        mockMvc.perform(
                        delete("/api/books/search/{type}/histories?keyword={keyword}", "GLOBAL", TEST_KEYWORD)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
    @DisplayName("검색어 삭제 실패 - 검색어 누락")
    void 검색기록삭제_검색어누락_실패() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/api/books/search/{type}/histories", "GLOBAL")
                                // keyword 파라미터 없음
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전체 검색 기록 삭제 성공")
    void 검색기록전체삭제_성공() throws Exception {
        // given
        doNothing().when(bookSearchFacade)
                .deleteAllSearchHistories(TEST_USER_ID, SearchType.GLOBAL);

        // when & then
        mockMvc.perform(
                        delete("/api/books/search/{type}/histories/all", "GLOBAL")
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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

        List<BookResponseDto.BookSearchDTO> books = bookCount > 0
                ? createMockBooks(bookCount)
                : Collections.emptyList();

        return BookResponseDto.SearchResultDto.builder()
                .totalResults(100L)
                .books(books)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private List<BookResponseDto.BookSearchDTO> createMockBooks(int count) {
        List<BookResponseDto.BookSearchDTO> books = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            books.add(BookResponseDto.BookSearchDTO.builder()
                    .isbn13(i == 0 ? TEST_ISBN_1 : TEST_ISBN_2)
                    .title("테스트 도서 " + (i + 1))
                    .mallType("도서")
                    .author("한강")
                    .publisher("창비")
                    .publicationDate("2024-01-01")
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .isInLibrary(false)
                    .build());
        }
        return books;
    }
}