 package app.nook.controller.book;

  import app.nook.book.controller.BookController;
  import app.nook.book.domain.enums.MallType;
  import app.nook.book.domain.enums.SourceType;
  import app.nook.book.dto.BookRequestDto;
  import app.nook.book.dto.BookResponseDto;
  import app.nook.book.exception.BookErrorCode;
  import app.nook.book.facade.UserBookFacade;
  import app.nook.book.service.BookService;
  import app.nook.global.common.AbstractWebMvcRestDocsTests;
  import app.nook.global.common.security.WithCustomUser;
  import app.nook.global.config.WebSecurityConfig;
  import app.nook.global.docs.ApiResponseSnippet;
  import app.nook.global.exception.CustomException;
  import app.nook.library.domain.enums.ReadingStatus;
  import app.nook.user.filter.JwtExceptionFilter;
  import app.nook.user.filter.JwtFilter;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
  import org.springframework.context.annotation.ComponentScan;
  import org.springframework.context.annotation.FilterType;
  import org.springframework.http.MediaType;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;

  import java.util.Arrays;
  import java.util.Collections;
  import java.util.List;

  import static org.mockito.ArgumentMatchers.*;
  import static org.mockito.BDDMockito.given;
  import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
  import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
  import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
  import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
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

      @MockitoBean
      private UserBookFacade userBookFacade;

      @Test
      @WithCustomUser
      @DisplayName("ISBN으로 도서 상세 조회 성공")
      void 도서_상세조회_성공() throws Exception {
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
                  .readingStatus(null)
                  .build();

          given(bookService.getBookDetailByIsbn(any(), eq(isbn13))).willReturn(response);

          mockMvc.perform(get("/api/v1/books/{isbn13}", isbn13).header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result.title").value("채식주의자"))
                  .andExpect(jsonPath("$.result.readingStatus").doesNotExist())
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          pathParameters(parameterWithName("isbn13").description("도서 ISBN13 (13자리 숫자)")),
                          responseFields(ApiResponseSnippet.withResult(
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
                                  fieldWithPath("result.bookShelfId").description("서재 ID (없으면 null)").optional(),
                                  fieldWithPath("result.readingStatus").description("독서 상태 (서재에 없으면 null)").optional()
                          ))
                  ));
      }

      @Test
      @WithCustomUser
      @DisplayName("잘못된 ISBN 형식으로 조회 시 400 Bad Request")
      void 도서_상세조회_실패_잘못된_ISBN_형식() throws Exception {
          mockMvc.perform(get("/api/v1/books/{isbn13}", "123").header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isBadRequest());
      }

      @Test
      @WithCustomUser
      @DisplayName("존재하지 않는 도서 조회 시 404 Not Found")
      void 도서_상세조회_실패_존재하지않는_도서() throws Exception {
          String isbn13 = "9999999999999";
          given(bookService.getBookDetailByIsbn(any(), eq(isbn13)))
                  .willThrow(new CustomException(BookErrorCode.BOOK_NOT_FOUND));

          mockMvc.perform(get("/api/v1/books/{isbn13}", isbn13).header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isNotFound());
      }

      @Test
      @WithCustomUser
      @DisplayName("bookId로 도서 상세 조회 성공")
      void 도서_상세조회_bookId_성공() throws Exception {
          BookResponseDto.BookDetailDto response = BookResponseDto.BookDetailDto.builder()
                  .bookId(1L)
                  .isbn13("9788936434267")
                  .title("테스트책")
                  .author("저자")
                  .publisher("출판사")
                  .publicationDate("2023-03-21")
                  .mallType("국내도서")
                  .mallTypeCode(MallType.BOOK)
                  .category("소설/시/희곡")
                  .pages(312)
                  .description("한강의 소설")
                  .coverImageUrl("http://example.com/cover.jpg")
                  .sourceType(SourceType.USER)
                  .bookShelfId(3L)
                  .readingStatus(ReadingStatus.READING)
                  .build();

          given(bookService.getBookDetailById(any(), eq(1L))).willReturn(response);

          mockMvc.perform(get("/api/v1/books/id/{bookId}", 1L).header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result.bookId").value(1L))
                  .andExpect(jsonPath("$.result.title").value("테스트책"))
                  .andExpect(jsonPath("$.result.readingStatus").value("READING"))
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          pathParameters(parameterWithName("bookId").description("도서 ID")),
                          responseFields(ApiResponseSnippet.withResult(
                                  fieldWithPath("result.bookId").description("도서 ID"),
                                  fieldWithPath("result.isbn13").description("ISBN13").optional(),
                                  fieldWithPath("result.title").description("도서 제목"),
                                  fieldWithPath("result.author").description("저자"),
                                  fieldWithPath("result.publisher").description("출판사").optional(),
                                  fieldWithPath("result.publicationDate").description("출판일").optional(),
                                  fieldWithPath("result.mallType").description("상품 유형").optional(),
                                  fieldWithPath("result.mallTypeCode").description("상품 유형 코드").optional(),
                                  fieldWithPath("result.category").description("카테고리").optional(),
                                  fieldWithPath("result.pages").description("페이지 수").optional(),
                                  fieldWithPath("result.description").description("도서 설명").optional(),
                                  fieldWithPath("result.coverImageUrl").description("표지 이미지 URL").optional(),
                                  fieldWithPath("result.aladinLink").description("알라딘 링크").optional(),
                                  fieldWithPath("result.sourceType").description("데이터 출처"),
                                  fieldWithPath("result.bookShelfId").description("서재 ID").optional(),
                                  fieldWithPath("result.readingStatus").description("독서 상태").optional()
                          ))
                  ));
      }

      @Test
      @WithCustomUser
      @DisplayName("주간 베스트셀러 목록 조회 성공")
      void 주간베스트셀러_조회_성공() throws Exception {
          List<BookResponseDto.BookPreviewDto> response = Arrays.asList(
                  new BookResponseDto.BookPreviewDto("9788936434267", "채식주의자", "한강", "http://example.com/cover1.jpg", "창비", 1),
                  new BookResponseDto.BookPreviewDto("9788936433598", "소년이 온다", "한강", "http://example.com/cover2.jpg", "창비", 2)
          );

          given(bookService.getWeeklyBestsellers()).willReturn(response);

          mockMvc.perform(get("/api/v1/books/bestsellers").header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result[0].title").value("채식주의자"))
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          responseFields(ApiResponseSnippet.withResult(
                                  fieldWithPath("result[].isbn13").description("ISBN13"),
                                  fieldWithPath("result[].title").description("도서 제목"),
                                  fieldWithPath("result[].author").description("저자"),
                                  fieldWithPath("result[].coverImageUrl").description("표지 이미지 URL"),
                                  fieldWithPath("result[].publisher").description("출판사"),
                                  fieldWithPath("result[].rank").description("베스트셀러 순위")
                          ))
                  ));
      }

      @Test
      @WithCustomUser
      @DisplayName("사용자 맞춤 추천 베스트셀러 조회 성공")
      void 추천베스트셀러_조회_성공() throws Exception {
          List<BookResponseDto.BookPreviewDto> response = Collections.singletonList(
                  new BookResponseDto.BookPreviewDto("9788936434267", "채식주의자", "한강", "http://example.com/cover1.jpg", "창비", 1)
          );

          given(bookService.getPersonalizedBestsellers(any())).willReturn(response);

          mockMvc.perform(get("/api/v1/books/recommendations").header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result[0].title").value("채식주의자"))
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          responseFields(ApiResponseSnippet.withResult(
                                  fieldWithPath("result[].isbn13").description("ISBN13"),
                                  fieldWithPath("result[].title").description("도서 제목"),
                                  fieldWithPath("result[].author").description("저자"),
                                  fieldWithPath("result[].coverImageUrl").description("표지 이미지 URL"),
                                  fieldWithPath("result[].publisher").description("출판사"),
                                  fieldWithPath("result[].rank").description("베스트셀러 순위")
                          ))
                  ));
      }

      @Test
      @WithCustomUser
      @DisplayName("사용자 도서 등록 성공")
      void 사용자_도서_등록_성공() throws Exception {
          BookResponseDto.BookDetailDto response = BookResponseDto.BookDetailDto.builder()
                  .bookId(101L)
                  .title("혼모노")
                  .author("성해은")
                  .category("소설/시/희곡")
                  .sourceType(SourceType.USER)
                  .bookShelfId(5L)
                  .readingStatus(ReadingStatus.BEFORE)
                  .build();

          given(userBookFacade.createUserBook(any(), any(BookRequestDto.CreateUserBookRequest.class)))
                  .willReturn(response);

          mockMvc.perform(post("/api/v1/books/user")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("""
                                  {
                                    "title":"혼모노",
                                    "author":"성해은",
                                    "categoryName":"소설/시/희곡",
                                    "description":"소개",
                                    "pages":348,
                                    "publisher":"민음사",
                                    "publicationDate":"2023-06-05",
                                    "isbn13":"9788936439743",
                                    "coverImageKey":"book/users/1/cover.png"
                                  }
                                  """)
                          .header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.code").value("SUCCESS-201"))
                  .andExpect(jsonPath("$.result.bookId").value(101L))
                  .andExpect(jsonPath("$.result.sourceType").value("USER"))
                  .andExpect(jsonPath("$.result.readingStatus").value("BEFORE"))
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          requestFields(
                                  fieldWithPath("title").description("도서 제목"),
                                  fieldWithPath("author").description("저자"),
                                  fieldWithPath("categoryName").description("카테고리명"),
                                  fieldWithPath("description").description("도서 설명").optional(),
                                  fieldWithPath("pages").description("페이지 수").optional(),
                                  fieldWithPath("publisher").description("출판사").optional(),
                                  fieldWithPath("publicationDate").description("출판일").optional(),
                                  fieldWithPath("isbn13").description("ISBN13").optional(),
                                  fieldWithPath("coverImageKey").description("표지 이미지 key").optional()
                          ),
                          responseFields(ApiResponseSnippet.withResult(
                                  fieldWithPath("result.bookId").description("도서 ID"),
                                  fieldWithPath("result.isbn13").description("ISBN13").optional(),
                                  fieldWithPath("result.title").description("도서 제목"),
                                  fieldWithPath("result.author").description("저자"),
                                  fieldWithPath("result.publisher").description("출판사").optional(),
                                  fieldWithPath("result.publicationDate").description("출판일").optional(),
                                  fieldWithPath("result.mallType").description("상품 유형").optional(),
                                  fieldWithPath("result.mallTypeCode").description("상품 유형 코드").optional(),
                                  fieldWithPath("result.category").description("카테고리").optional(),
                                  fieldWithPath("result.pages").description("페이지 수").optional(),
                                  fieldWithPath("result.description").description("도서 설명").optional(),
                                  fieldWithPath("result.coverImageUrl").description("표지 이미지 URL").optional(),
                                  fieldWithPath("result.aladinLink").description("알라딘 링크").optional(),
                                  fieldWithPath("result.sourceType").description("데이터 출처"),
                                  fieldWithPath("result.bookShelfId").description("서재 ID").optional(),
                                  fieldWithPath("result.readingStatus").description("독서 상태").optional()
                          ))
                  ));
      }

      @Test
      @WithCustomUser
      @DisplayName("사용자 도서 수정 성공")
      void 사용자_도서_수정_성공() throws Exception {
          BookResponseDto.BookDetailDto response = BookResponseDto.BookDetailDto.builder()
                  .isbn13("1721329381232")
                  .bookId(101L)
                  .title("혼모노 수정")
                  .author("성해은")
                  .publisher("창비")
                  .publicationDate("2012-02-03")
                  .category("소설/시/희곡")
                  .pages(212)
                  .description("혼모노 수정")
                  .coverImageUrl("http://example.com/cover.jpg")
                  .sourceType(SourceType.USER)
                  .bookShelfId(5L)
                  .readingStatus(ReadingStatus.READING)
                  .build();

          given(userBookFacade.updateUserBook(any(), anyLong(), any(BookRequestDto.UpdateUserBookRequest.class)))
                  .willReturn(response);

          mockMvc.perform(patch("/api/v1/books/user/{bookId}", 101L)
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("""
                                  {
                                    "title":"혼모노 수정",
                                    "author":"성해은",
                                    "categoryName":"소설/시/희곡",
                                    "description":"소개수정",
                                    "pages":360,
                                    "publisher":"민음사",
                                    "publicationDate":"2024-01-01",
                                    "isbn13":"9788936439743",
                                    "coverImageKey":"book/users/1/cover2.png"
                                  }
                                  """)
                          .header(AUTH_HEADER, AUTH_TOKEN))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result.bookId").value(101L))
                  .andExpect(jsonPath("$.result.readingStatus").value("READING"))
                  .andDo(documentWithAuth(
                          "{class-name}/{method-name}",
                          pathParameters(
                                  parameterWithName("bookId").description("수정할 사용자 도서 ID")
                          ),
                          requestFields(
                                  fieldWithPath("title").description("도서 제목"),
                                  fieldWithPath("author").description("저자"),
                                  fieldWithPath("categoryName").description("카테고리명"),
                                  fieldWithPath("description").description("도서 설명").optional(),
                                  fieldWithPath("pages").description("페이지 수").optional(),
                                  fieldWithPath("publisher").description("출판사").optional(),
                                  fieldWithPath("publicationDate").description("출판일").optional(),
                                  fieldWithPath("isbn13").description("ISBN13").optional(),
                                  fieldWithPath("coverImageKey").description("표지 이미지 key").optional()
                          ),
                          responseFields(ApiResponseSnippet.withResult(
                                  fieldWithPath("result.bookId").description("도서 ID"),
                                  fieldWithPath("result.isbn13").description("ISBN13").optional(),
                                  fieldWithPath("result.title").description("도서 제목"),
                                  fieldWithPath("result.author").description("저자"),
                                  fieldWithPath("result.publisher").description("출판사").optional(),
                                  fieldWithPath("result.publicationDate").description("출판일").optional(),
                                  fieldWithPath("result.mallType").description("상품 유형").optional(),
                                  fieldWithPath("result.mallTypeCode").description("상품 유형 코드").optional(),
                                  fieldWithPath("result.category").description("카테고리").optional(),
                                  fieldWithPath("result.pages").description("페이지 수").optional(),
                                  fieldWithPath("result.description").description("도서 설명").optional(),
                                  fieldWithPath("result.coverImageUrl").description("표지 이미지 URL").optional(),
                                  fieldWithPath("result.aladinLink").description("알라딘 링크").optional(),
                                  fieldWithPath("result.sourceType").description("데이터 출처"),
                                  fieldWithPath("result.bookShelfId").description("서재 ID").optional(),
                                  fieldWithPath("result.readingStatus").description("독서 상태").optional()
                          ))
                  ));
      }
  }
