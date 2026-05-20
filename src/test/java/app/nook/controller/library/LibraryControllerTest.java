package app.nook.controller.library;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.dto.CursorResponse;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.controller.LibraryController;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.service.LibraryCommandService;
import app.nook.library.service.LibraryQueryService;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(
        controllers = LibraryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class LibraryControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private LibraryCommandService libraryCommandService;

    @MockitoBean
    private LibraryQueryService libraryQueryService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("서재에 책 등록 성공")
    @WithCustomUser
    void 서재_책_등록_성공() throws Exception {
        LibraryViewDto.BookStatusResponseDto response =
                new LibraryViewDto.BookStatusResponseDto(1L, 10L, ReadingStatus.BEFORE);

        given(libraryCommandService.registerBook(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/v1/library/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bookId").value(1L))
                .andExpect(jsonPath("$.result.bookShelfId").value(10L))
                .andExpect(jsonPath("$.result.readingStatus").value("BEFORE"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에 추가할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.bookId").type(NUMBER).description("도서 ID"),
                                fieldWithPath("result.bookShelfId").type(NUMBER).description("서재 ID"),
                                fieldWithPath("result.readingStatus").type(STRING).description("독서 상태")
                        ))
                ));
    }

    @Test
    @DisplayName("서재에서 책 삭제 성공")
    @WithCustomUser
    void 서재_책_삭제_성공() throws Exception {
        LibraryViewDto.BookStatusResponseDto response =
                new LibraryViewDto.BookStatusResponseDto(1L, null, null);

        given(libraryCommandService.deleteByBookId(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(
                        delete("/api/v1/library/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bookId").value(1L))
                .andExpect(jsonPath("$.result.bookShelfId").value(nullValue()))
                .andExpect(jsonPath("$.result.readingStatus").value(nullValue()))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에서 삭제할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.bookId").type(NUMBER).description("도서 ID"),
                                fieldWithPath("result.bookShelfId").type(NULL).description("서재 ID (삭제 후 null)"),
                                fieldWithPath("result.readingStatus").type(NULL).description("독서 상태 (삭제 후 null)")
                        ))
                ));
    }

    @Test
    @DisplayName("서재 책 상태변경 성공")
    @WithCustomUser
    void 서재_책_상태변경_성공() throws Exception {
        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);
        LibraryViewDto.BookStatusResponseDto response =
                new LibraryViewDto.BookStatusResponseDto(1L, 10L, ReadingStatus.READING);

        given(libraryCommandService.changeReadingStatus(anyLong(), any())).willReturn(response);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bookId").value(1L))
                .andExpect(jsonPath("$.result.bookShelfId").value(10L))
                .andExpect(jsonPath("$.result.readingStatus").value("READING"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("bookId").description("상태 변경할 도서 ID"),
                                fieldWithPath("readingStatus").description("독서 상태 (READING, FINISHED, BEFORE)")
                        ),
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.bookId").type(NUMBER).description("도서 ID"),
                                fieldWithPath("result.bookShelfId").type(NUMBER).description("서재 ID"),
                                fieldWithPath("result.readingStatus").type(STRING).description("변경된 독서 상태")
                        ))
                ));
    }

    @Test
    @WithCustomUser
    void 서재_책_상태변경_실패_낙관적잠금충돌() throws Exception {
        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

        willThrow(new ObjectOptimisticLockingFailureException("Library", 1L))
                .given(libraryCommandService).changeReadingStatus(anyLong(), any());

        mockMvc.perform(
                        patch("/api/v1/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-004"));
    }

    @Test
    @WithCustomUser
    void 서재_책_상태변경_실패_bookId_누락() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "readingStatus": "READING"
                                        }
                                        """)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @WithCustomUser
    void 서재_책_상태변경_실패_readingStatus_누락() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "bookId": 1
                                        }
                                        """)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("서재 상태별 책 조회 성공")
    @WithCustomUser
    void 서재_상태별_책_조회_성공() throws Exception {
        LibraryViewDto.UserStatusBookItem item =
                new LibraryViewDto.ReadingBookItem(
                        1L,
                        "타이틀",
                        "작가",
                        "https://example.com/cover.jpg",
                        LocalDate.of(2025, 1, 1)
                );

        CursorResponse<LibraryViewDto.UserStatusBookItem, Long> cursorResponse =
                CursorResponse.of(List.of(item), 10L, true);

        LibraryViewDto.StatusBookResponseDto response =
                new LibraryViewDto.StatusBookResponseDto(
                        ReadingStatus.READING,
                        1,
                        cursorResponse
                );

        given(libraryQueryService.getBooksByStatus(anyLong(), any(), any(), anyInt()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("status", "READING")
                                .param("cursor", "10")
                                .param("size", "2")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        queryParameters(
                                parameterWithName("status").description("독서 상태 (BEFORE, READING, FINISHED)"),
                                parameterWithName("cursor").optional().description("커서(마지막 ID). 최초 조회 시 미전달"),
                                parameterWithName("size").description("조회할 개수")
                        ),
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.readingStatus").type(STRING).description("독서 상태"),
                                fieldWithPath("result.totalBookNum").type(NUMBER).description("해당 상태의 전체 책 수 (첫 조회 시만 제공)"),
                                fieldWithPath("result.bookItems").type(OBJECT).description("커서 기반 응답"),
                                fieldWithPath("result.bookItems.items[]").type(ARRAY).description("도서 목록"),
                                fieldWithPath("result.bookItems.items[].bookId").type(NUMBER).description("도서 ID"),
                                fieldWithPath("result.bookItems.items[].title").type(STRING).description("도서 제목"),
                                fieldWithPath("result.bookItems.items[].author").type(STRING).description("도서 작가"),
                                fieldWithPath("result.bookItems.items[].coverUrl").type(STRING).description("도서 커버 이미지 URL"),
                                fieldWithPath("result.bookItems.items[].startedAt").type(STRING).optional().description("읽기 시작일 (READING/FINISHED)"),
                                fieldWithPath("result.bookItems.items[].endedAt").type(STRING).optional().description("완독일 (FINISHED)"),
                                fieldWithPath("result.bookItems.nextCursor").type(NUMBER).description("다음 커서"),
                                fieldWithPath("result.bookItems.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        ))
                ));
    }

    @Test
    @WithCustomUser
    void 서재_상태별_책_조회_실패_status_누락() throws Exception {
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("size", "20")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}"
                ));

        verifyNoInteractions(libraryCommandService, libraryQueryService);
    }

    @Test
    @WithCustomUser
    void 서재_상태별_책_조회_실패_cursor_0() throws Exception {
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("status", "READING")
                                .param("cursor", "0")
                                .param("size", "20")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}"
                ));

        verifyNoInteractions(libraryCommandService, libraryQueryService);
    }

    @Test
    @WithCustomUser
    void 서재_상태별_책_조회_실패_size_최소값미만() throws Exception {
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("status", "READING")
                                .param("size", "0")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}"
                ));

        verifyNoInteractions(libraryCommandService, libraryQueryService);
    }

    @Test
    @WithCustomUser
    void 서재_상태별_책_조회_실패_size_최대값초과() throws Exception {
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("status", "READING")
                                .param("size", "101")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isBadRequest())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}"
                ));

        verifyNoInteractions(libraryCommandService, libraryQueryService);
    }

    @Test
    void 서재_상태별_책_조회_실패_인증없음() throws Exception {
        mockMvc.perform(
                        get("/api/v1/library/status")
                                .param("status", "READING")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithCustomUser
    void 읽기전_상태_책_5권_조회_성공() throws Exception {
        LibraryViewDto.BeforeReadingResponseDto response = new LibraryViewDto.BeforeReadingResponseDto(
                List.of(
                        new LibraryViewDto.BeforeBookItem(1L, "제목1", "저자1", "https://example.com/1.jpg"),
                        new LibraryViewDto.BeforeBookItem(2L, "제목2", "저자2", "https://example.com/2.jpg")
                )
        );

        given(libraryQueryService.getBeforeReadingBooks(anyLong())).willReturn(response);

        mockMvc.perform(
                        get("/api/v1/library/before-reading")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.books").type(ARRAY).description("읽기 전 상태 도서 목록 (최대 5권)"),
                                fieldWithPath("result.books[].bookId").type(NUMBER).description("도서 ID"),
                                fieldWithPath("result.books[].title").type(STRING).description("도서 제목"),
                                fieldWithPath("result.books[].author").type(STRING).description("도서 저자"),
                                fieldWithPath("result.books[].coverUrl").type(STRING).description("도서 커버 URL")
                        ))
                ));
    }

    @DisplayName("최근 포커스 도서 조회")
    @Nested
    class ViewRecentFocus {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @WithCustomUser
            void 최근_포커스_도서_조회_데이터있음_200() throws Exception {
                LibraryViewDto.RecentFocusResponseDto response = new LibraryViewDto.RecentFocusResponseDto(
                        1L,
                        "https://example.com/cover.jpg",
                        "타이틀",
                        12,
                        "01:30:00"
                );

                given(libraryQueryService.getRecentFocus(anyLong())).willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/recent-focus")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/최근_포커스_도서_조회_성공",
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.bookId").type(NUMBER).description("도서 ID"),
                                        fieldWithPath("result.coverUrl").type(STRING).description("도서 커버 이미지 URL"),
                                        fieldWithPath("result.title").type(STRING).description("도서 제목"),
                                        fieldWithPath("result.page").type(NUMBER).optional().description("최근 포커스 시 기록된 페이지 (없으면 null)"),
                                        fieldWithPath("result.focusTime").type(STRING).description("누적 포커스 시간 (HH:mm:ss 형식)")
                                ))
                        ));
            }

            @Test
            @WithCustomUser
            void 최근_포커스_도서_조회_데이터없음_빈응답_200() throws Exception {
                given(libraryQueryService.getRecentFocus(anyLong())).willReturn(null);

                mockMvc.perform(
                                get("/api/v1/library/recent-focus")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/최근_포커스_도서_조회_데이터없음_빈응답_200",
                                responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                        ));
            }
        }
    }

    @DisplayName("서재 책 개수 조회")
    @Nested
    class ViewBookCount {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("서재 책 개수 조회")
            @WithCustomUser
            void viewBookCount() throws Exception {
                given(libraryQueryService.getBookCount(anyLong()))
                        .willReturn(new LibraryViewDto.BookCountResponseDto(42));

                mockMvc.perform(
                                get("/api/v1/library/count")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.totalBookNum").type(NUMBER).description("서재 전체 책 수")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("인증 정보가 없으면 401")
            void viewBookCountWithoutAuth() throws Exception {
                mockMvc.perform(get("/api/v1/library/count"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @DisplayName("독서 연도 조회")
    @Nested
    class ViewReadingYears {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("가입 연도부터 현재 연도까지 연도 목록 반환")
            @WithCustomUser
            void viewReadingYears() throws Exception {
                // given
                LibraryViewDto.YearResponseDto response =
                        new LibraryViewDto.YearResponseDto(List.of(2024, 2025, 2026));

                given(libraryQueryService.getReadingYears(anyLong())).willReturn(response);

                // when & then
                mockMvc.perform(
                                get("/api/v1/library/years")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result.years").isArray())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.years").type(ARRAY).description("가입 연도부터 현재까지의 연도 목록 (오름차순)")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("인증 정보가 없으면 401")
            void viewReadingYearsWithoutAuth() throws Exception {
                mockMvc.perform(get("/api/v1/library/years"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @DisplayName("날짜별 포커스 기록 조회")
    @Nested
    class ViewFocusRecordByDate {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("해당 날짜 포커스 기록 조회")
            @WithCustomUser
            void focusRecordByDate() throws Exception {
                LibraryViewDto.UserBookResponseDto item = new LibraryViewDto.UserBookResponseDto(
                        1L,
                        "타이틀",
                        "작가",
                        "00:30:00",
                        "https://example.com/cover.jpg"
                );

                CursorResponse<LibraryViewDto.UserBookResponseDto, Long> response =
                        CursorResponse.of(List.of(item), 77L, true);

                given(libraryQueryService.getFocusRecordsByDate(anyLong(), any(LocalDate.class), any(), anyInt()))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/focus-records")
                                        .param("date", "2026-02-26")
                                        .param("cursor", "100")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                queryParameters(
                                        parameterWithName("date").description("조회 날짜 (yyyy-MM-dd)"),
                                        parameterWithName("cursor").optional().description("커서(마지막 focus ID). 최초 조회 시 미전달"),
                                        parameterWithName("size").optional().description("조회할 개수 (기본값: 4)")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.items").type(ARRAY).description("포커스 도서 목록"),
                                        fieldWithPath("result.items[].bookId").type(NUMBER).description("도서 ID"),
                                        fieldWithPath("result.items[].title").type(STRING).description("도서 제목"),
                                        fieldWithPath("result.items[].author").type(STRING).description("도서 저자"),
                                        fieldWithPath("result.items[].focusTime").type(STRING).description("해당 포커스 시간 (HH:mm:ss 형식)"),
                                        fieldWithPath("result.items[].coverUrl").type(STRING).description("도서 커버 URL"),
                                        fieldWithPath("result.nextCursor").type(NUMBER).description("다음 커서"),
                                        fieldWithPath("result.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("date 형식이 잘못되면 400")
            @WithCustomUser
            void invalidDateFormat() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/focus-records")
                                        .param("date", "2026/02/26")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isBadRequest())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}"
                        ));
            }

            @Test
            @DisplayName("cursor가 0이면 400")
            @WithCustomUser
            void invalidCursorZero() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/focus-records")
                                        .param("date", "2026-02-26")
                                        .param("cursor", "0")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isBadRequest())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}"
                        ));

                verifyNoInteractions(libraryCommandService, libraryQueryService);
            }
        }
    }
}
