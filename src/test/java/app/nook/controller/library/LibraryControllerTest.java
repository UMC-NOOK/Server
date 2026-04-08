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
import app.nook.library.service.LibraryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
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
    private LibraryService libraryService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithCustomUser
    void 서재_책_등록_성공() throws Exception {
        willDoNothing().given(libraryService).save(any(), anyLong());

        // when & then
        mockMvc.perform(
                        post("/api/v1/library/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에 추가할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }

    @Test
    @WithCustomUser
    void 서재_책_삭제_성공() throws Exception {
        willDoNothing().given(libraryService).deleteById(any(), anyLong());

        // when & then
        mockMvc.perform(
                        delete("/api/v1/library/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에서 삭제할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }

    @Test
    @WithCustomUser
    void 서재_책_상태변경_성공() throws Exception {
        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

        willDoNothing().given(libraryService).changeStatus(any(), any());

        // when & then
        mockMvc.perform(
                        patch("/api/v1/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("bookId").description("상태 변경할 도서 ID"),
                                fieldWithPath("readingStatus").description("독서 상태 (READING, FINISHED, BEFORE)")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }

    @Test
    @WithCustomUser
    void 서재_책_상태변경_실패_낙관적잠금충돌() throws Exception {
        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

        willThrow(new ObjectOptimisticLockingFailureException("Library", 1L))
                .given(libraryService).changeStatus(any(), any());

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
    void 서재_상태별_책_조회_성공() throws Exception {
        LibraryViewDto.UserStatusBookItem item =
                new LibraryViewDto.ReadingBookItem(
                        1L,
                        "타이틀",
                        "작가",
                        "https://example.com/cover.jpg",
                        LocalDate.of(2025, 1, 1)
                );

        CursorResponse<LibraryViewDto.UserStatusBookItem> cursorResponse =
                CursorResponse.of(List.of(item), 10L, true);

        LibraryViewDto.StatusBookResponseDto response =
                new LibraryViewDto.StatusBookResponseDto(
                        ReadingStatus.READING,
                        1,
                        cursorResponse
                );

        given(libraryService.viewBooksByStatus(any(), any(), any(), anyInt()))
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
    void 읽기전_상태_책_5권_조회_성공() throws Exception {
        LibraryViewDto.BeforeReadingResponseDto response = new LibraryViewDto.BeforeReadingResponseDto(
                List.of(
                        new LibraryViewDto.BeforeBookItem(1L, "제목1", "저자1", "https://example.com/1.jpg"),
                        new LibraryViewDto.BeforeBookItem(2L, "제목2", "저자2", "https://example.com/2.jpg")
                )
        );

        given(libraryService.viewBeforeReadingBooks(any())).willReturn(response);

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
                        "타이틀",
                        12
                );

                given(libraryService.viewRecentFocus(any())).willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/recent-focus")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/최근_포커스_도서_조회_성공",
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.bookId").type(NUMBER).description("도서 ID"),
                                        fieldWithPath("result.title").type(STRING).description("도서 제목"),
                                        fieldWithPath("result.page").type(NUMBER).optional().description("최근 포커스 시 기록된 페이지 (없으면 null)")
                                ))
                        ));
            }

            @Test
            @WithCustomUser
            void 최근_포커스_도서_조회_데이터없음_204() throws Exception {
                given(libraryService.viewRecentFocus(any())).willReturn(null);

                mockMvc.perform(
                                get("/api/v1/library/recent-focus")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/최근_포커스_도서_조회_데이터없음_204",
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
                given(libraryService.countBooks(any()))
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
                        1800,
                        "https://example.com/cover.jpg"
                );

                CursorResponse<LibraryViewDto.UserBookResponseDto> response =
                        CursorResponse.of(List.of(item), 77L, true);

                given(libraryService.viewFocusRecordByDate(any(), any(LocalDate.class), any(), anyInt()))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/focus-records")
                                        .param("date", "2026-02-26")
                                        .param("cursor", "100")
                                        .param("size", "20")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                queryParameters(
                                        parameterWithName("date").description("조회 날짜 (yyyy-MM-dd)"),
                                        parameterWithName("cursor").optional().description("커서(마지막 focus ID). 최초 조회 시 미전달"),
                                        parameterWithName("size").description("조회할 개수")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.items").type(ARRAY).description("포커스 도서 목록"),
                                        fieldWithPath("result.items[].bookId").type(NUMBER).description("도서 ID"),
                                        fieldWithPath("result.items[].title").type(STRING).description("도서 제목"),
                                        fieldWithPath("result.items[].author").type(STRING).description("도서 저자"),
                                        fieldWithPath("result.items[].focusSec").type(NUMBER).description("해당 포커스 시간(초)"),
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
                        .andExpect(status().isBadRequest());
            }
        }
    }
}
