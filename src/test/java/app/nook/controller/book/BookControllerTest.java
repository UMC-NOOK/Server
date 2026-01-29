package app.nook.controller.book;

import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.service.BookService;
import app.nook.global.common.AbstractRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class BookControllerTest extends AbstractRestDocsTests {

    @MockitoBean
    private BookService bookService;

    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUpUser() {
        user = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        userDetails = new CustomUserDetails(user);
    }

    @Test
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

        given(bookService.getBookDetailByIsbn(any(Long.class), eq(isbn13)))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", isbn13)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
    void 도서_상세조회_실패_잘못된_ISBN_형식() throws Exception {
        // given
        String invalidIsbn = "123"; // 13자리가 아님

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", invalidIsbn)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void 도서_상세조회_실패_존재하지않는_도서() throws Exception {
        // given
        String isbn13 = "9999999999999";

        given(bookService.getBookDetailByIsbn(any(Long.class), eq(isbn13)))
                .willThrow(new CustomException(ErrorCode.BOOK_NOT_FOUND));

        // when & then
        mockMvc.perform(
                        get("/api/books/{isbn13}", isbn13)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void 주간베스트셀러_조회_성공() throws Exception {
        // given
        List<BookResponseDto.BookPreviewDto> response = Arrays.asList(
                BookResponseDto.BookPreviewDto.builder()
                        .isbn13("9788936434267")
                        .title("채식주의자")
                        .author("한강")
                        .coverImageUrl("http://example.com/cover1.jpg")
                        .publisher("창비")
                        .rank(1)
                        .build(),
                BookResponseDto.BookPreviewDto.builder()
                        .isbn13("9788936433598")
                        .title("소년이 온다")
                        .author("한강")
                        .coverImageUrl("http://example.com/cover2.jpg")
                        .publisher("창비")
                        .rank(2)
                        .build()
        );

        given(bookService.getWeeklyBestsellers())
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/bestsellers")
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
    void 추천베스트셀러_조회_성공() throws Exception {
        // given
        List<BookResponseDto.BookPreviewDto> response = Collections.singletonList(
                BookResponseDto.BookPreviewDto.builder()
                        .isbn13("9788936434267")
                        .title("채식주의자")
                        .author("한강")
                        .coverImageUrl("http://example.com/cover1.jpg")
                        .publisher("창비")
                        .rank(1)
                        .build()
        );

        given(bookService.getPersonalizedBestsellers(any(Long.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/books/recommendations")
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
