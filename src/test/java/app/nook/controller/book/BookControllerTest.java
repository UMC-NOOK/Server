package app.nook.controller.book;

import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.controller.BookController;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.service.BookService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
public class BookControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private BookService bookService;

    @Test
    @WithCustomUser
    @DisplayName("ISBN으로 도서 상세 조회 성공")
    void 도서_상세조회_성공() throws Exception {
        // given
        String isbn13 = "9788936434267";

        BookResponseDto.BookDetailDto response = BookResponseDto.BookDetailDto.builder()
                .bookId(1L)
                .isbn13(isbn13)
                .title("채식주의자")
                .author("한강")
                .publisher("창비")
                .publicationDate("2007-10-30")
                .mallType("도서")
                .mallTypeCode(MallType.BOOK)
                .category("소설")
                .pages(184)
                .description("한강의 소설")
                .coverImageUrl("http://example.com/cover.jpg")
                .aladinLink("http://aladin.com/book")
                .sourceType(SourceType.ALADIN)
                .bookShelfId(null)
                .build();

        given(bookService.getBookDetailByIsbn(any(), eq(isbn13)))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", isbn13)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("채식주의자"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("isbn13").description("도서 ISBN13 (13자리 숫자)")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.bookId").description("도서 ID"),
                                        fieldWithPath("result.isbn13").description("ISBN13"),
                                        fieldWithPath("result.title").description("도서 제목"),
                                        fieldWithPath("result.author").description("저자"),
                                        fieldWithPath("result.publisher").description("출판사"),
                                        fieldWithPath("result.publicationDate").description("출판일"),
                                        fieldWithPath("result.mallType").description("상품 유형"),
                                        fieldWithPath("result.mallTypeCode").description("상품 유형 코드"),
                                        fieldWithPath("result.category").description("카테고리"),
                                        fieldWithPath("result.pages").description("페이지 수"),
                                        fieldWithPath("result.description").description("도서 설명"),
                                        fieldWithPath("result.coverImageUrl").description("표지 이미지 URL"),
                                        fieldWithPath("result.aladinLink").description("알라딘 링크"),
                                        fieldWithPath("result.sourceType").description("데이터 출처"),
                                        fieldWithPath("result.bookShelfId").description("서재 ID (없으면 null)").optional()
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("잘못된 ISBN 형식으로 조회 시 400 Bad Request")
    void 도서_상세조회_실패_잘못된_ISBN_형식() throws Exception {
        // given
        String invalidIsbn = "123"; // 13자리가 아님

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", invalidIsbn)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("존재하지 않는 도서 조회 시 404 Not Found")
    void 도서_상세조회_실패_존재하지않는_도서() throws Exception {
        // given
        String isbn13 = "9999999999999";

        given(bookService.getBookDetailByIsbn(any(), eq(isbn13)))
                .willThrow(new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", isbn13)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @WithCustomUser
    @DisplayName("주간 베스트셀러 목록 조회 성공")
    void 주간베스트셀러_조회_성공() throws Exception {
        // given
        List<BookResponseDto.BookPreviewDto> response = Arrays.asList(
                new BookResponseDto.BookPreviewDto(
                        "9788936434267",
                        "채식주의자",
                        "한강",
                        "http://example.com/cover1.jpg",
                        "창비",
                        1
                ),
                new BookResponseDto.BookPreviewDto(
                        "9788936433598",
                        "소년이 온다",
                        "한강",
                        "http://example.com/cover2.jpg",
                        "창비",
                        2
                )
        );

        given(bookService.getWeeklyBestsellers())
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/bestsellers")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].title").value("채식주의자"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result[].isbn13").description("ISBN13"),
                                        fieldWithPath("result[].title").description("도서 제목"),
                                        fieldWithPath("result[].author").description("저자"),
                                        fieldWithPath("result[].coverImageUrl").description("표지 이미지 URL"),
                                        fieldWithPath("result[].publisher").description("출판사"),
                                        fieldWithPath("result[].rank").description("베스트셀러 순위")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("사용자 맞춤 추천 베스트셀러 조회 성공")
    void 추천베스트셀러_조회_성공() throws Exception {
        // given
        List<BookResponseDto.BookPreviewDto> response = Collections.singletonList(
                new BookResponseDto.BookPreviewDto(
                    "9788936434267",
                        "채식주의자",
                        "한강",
                        "http://example.com/cover1.jpg",
                        "창비",
                        1

                )
        );

        given(bookService.getPersonalizedBestsellers(any(Long.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/recommendations")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].title").value("채식주의자"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result[].isbn13").description("ISBN13"),
                                        fieldWithPath("result[].title").description("도서 제목"),
                                        fieldWithPath("result[].author").description("저자"),
                                        fieldWithPath("result[].coverImageUrl").description("표지 이미지 URL"),
                                        fieldWithPath("result[].publisher").description("출판사"),
                                        fieldWithPath("result[].rank").description("베스트셀러 순위")
                                )
                        )
                ));
    }
}
