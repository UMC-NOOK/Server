package app.nook.controller.focus;

import app.nook.focus.controller.FocusController;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.service.FocusQueryService;
import app.nook.focus.service.FocusService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FocusController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class FocusControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private FocusService focusService;

    @MockitoBean
    private FocusQueryService focusQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("포커스 시작")
    class StartFocus {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(20L);

            FocusResponseDto.FocusStart response = new FocusResponseDto.FocusStart(
                    100L,
                    20L,
                    "첫사랑의 침공",
                    "권혁일",
                    LocalDateTime.of(2026, 3, 22, 14, 0, 0)
            );

            given(focusService.startFocus(any(User.class), eq(request))).willReturn(response);

            mockMvc.perform(
                            post("/api/v1/focuses/start")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS-201"))
                    .andExpect(jsonPath("$.result.focusId").value(100))
                    .andExpect(jsonPath("$.result.bookId").value(20))
                    .andExpect(jsonPath("$.result.bookTitle").value("첫사랑의 침공"))
                    .andExpect(jsonPath("$.result.author").value("권혁일"))
                    .andExpect(jsonPath("$.result.startedAt").value("2026-03-22T14:00:00"))
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_시작_성공",
                            requestFields(
                                    fieldWithPath("bookId").description("책 ID")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.focusId").description("포커스 ID"),
                                            fieldWithPath("result.bookId").description("책 ID"),
                                            fieldWithPath("result.bookTitle").description("책 제목"),
                                            fieldWithPath("result.author").description("저자"),
                                            fieldWithPath("result.startedAt").description("포커스 시작 시각")
                                    )
                            )
                    ));
        }

        @Test
        @WithCustomUser
        @DisplayName("이미 진행 중이면 FOCUS-001 충돌 응답을 반환한다")
        void rejectsAlreadyInProgress() throws Exception {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(20L);

            willThrow(new CustomException(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS))
                    .given(focusService)
                    .startFocus(any(User.class), eq(request));

            mockMvc.perform(
                            post("/api/v1/focuses/start")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("FOCUS-001"))
                    .andExpect(jsonPath("$.message").value("이미 진행 중인 포커스가 있습니다."))
                    .andExpect(jsonPath("$.result").doesNotExist())
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_시작_실패_이미_진행중",
                            requestFields(
                                    fieldWithPath("bookId").description("책 ID")
                            ),
                            responseFields(ApiResponseSnippet.failureResponseFields())
                    ));
        }

        @Test
        @WithCustomUser
        @DisplayName("bookId의 누락 및 잘못된 경계값은 COMMON-002로 거절한다")
        void rejectsInvalidBookIdBoundaries() throws Exception {
            List<String> invalidRequests = List.of(
                    "{}",
                    "{\"bookId\": 0}"
            );

            for (String invalidRequest : invalidRequests) {
                mockMvc.perform(
                                post("/api/v1/focuses/start")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invalidRequest)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("COMMON-002"));
            }
        }
    }

    @Nested
    @DisplayName("포커스 종료")
    class EndFocus {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 72, true);

            FocusResponseDto.FocusEnd response = new FocusResponseDto.FocusEnd(
                    100L,
                    20L,
                    LocalDateTime.of(2026, 8, 1, 23, 0, 0),
                    LocalDateTime.of(2026, 8, 2, 0, 30, 0),
                    5400,
                    "01:30:00",
                    72,
                    9000L,
                    "FINISHED"
            );

            given(focusService.endFocus(anyLong(), eq(request))).willReturn(response);

            mockMvc.perform(
                            post("/api/v1/focuses/end")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.*").value(hasSize(9)))
                    .andExpect(jsonPath("$.result.focusId").value(100))
                    .andExpect(jsonPath("$.result.bookId").value(20))
                    .andExpect(jsonPath("$.result.startedAt").value("2026-08-01T23:00:00"))
                    .andExpect(jsonPath("$.result.endedAt").value("2026-08-02T00:30:00"))
                    .andExpect(jsonPath("$.result.durationSec").value(5400))
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_종료_성공",
                            requestFields(
                                    fieldWithPath("focusId").description("종료할 포커스 ID"),
                                    fieldWithPath("page").type(NUMBER).optional().description("현재까지 읽은 페이지 (미전달 가능, 없으면 null)"),
                                    fieldWithPath("isFinished").description("완독 여부")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.focusId").description("종료를 요청한 원본 포커스 ID"),
                                            fieldWithPath("result.bookId").description("책 ID"),
                                            fieldWithPath("result.startedAt").description("전체 종료 작업의 포커스 시작 시각"),
                                            fieldWithPath("result.endedAt").description("전체 종료 작업의 포커스 종료 시각"),
                                            fieldWithPath("result.durationSec").description("전체 종료 작업의 집중 시간(초)"),
                                            fieldWithPath("result.durationText").description("전체 종료 작업의 집중 시간(HH:mm:ss)"),
                                            fieldWithPath("result.page").type(NUMBER).optional().description("작업 완료 후 최종 서재의 현재까지 읽은 페이지 (없으면 null)"),
                                            fieldWithPath("result.totalFocusSec").description("작업 완료 후 최종 서재의 누적 포커스 시간(초)"),
                                            fieldWithPath("result.readingStatus").description("작업 완료 후 최종 서재의 독서 상태")
                                    )
                            )
                    ));
        }

        @Test
        @WithCustomUser
        @DisplayName("page를 생략해도 종료 요청을 전달한다")
        void acceptsOmittedPage() throws Exception {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, null, false);
            FocusResponseDto.FocusEnd response = new FocusResponseDto.FocusEnd(
                    100L,
                    20L,
                    LocalDateTime.of(2026, 8, 1, 23, 0, 0),
                    LocalDateTime.of(2026, 8, 2, 0, 30, 0),
                    5400,
                    "01:30:00",
                    null,
                    9000L,
                    "READING"
            );
            given(focusService.endFocus(anyLong(), eq(request))).willReturn(response);

            mockMvc.perform(
                            post("/api/v1/focuses/end")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"focusId\": 100, \"isFinished\": false}")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.page").doesNotExist());
        }

        @Test
        @WithCustomUser
        @DisplayName("이미 종료되었으면 FOCUS-003 충돌 응답을 반환한다")
        void rejectsAlreadyEnded() throws Exception {
            FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 72, true);

            willThrow(new CustomException(FocusErrorCode.FOCUS_ALREADY_ENDED))
                    .given(focusService)
                    .endFocus(anyLong(), eq(request));

            mockMvc.perform(
                            post("/api/v1/focuses/end")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("FOCUS-003"))
                    .andExpect(jsonPath("$.message").value("이미 종료된 포커스입니다."))
                    .andExpect(jsonPath("$.result").doesNotExist())
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_종료_실패_이미_종료됨",
                            requestFields(
                                    fieldWithPath("focusId").description("종료할 포커스 ID"),
                                    fieldWithPath("page").type(NUMBER).optional().description("현재까지 읽은 페이지 (미전달 가능, 없으면 null)"),
                                    fieldWithPath("isFinished").description("완독 여부")
                            ),
                            responseFields(ApiResponseSnippet.failureResponseFields())
                    ));
        }

        @Test
        @WithCustomUser
        @DisplayName("focusId, page, isFinished의 잘못된 경계값은 COMMON-002로 거절한다")
        void rejectsInvalidEndRequestBoundaries() throws Exception {
            List<String> invalidRequests = List.of(
                    "{\"page\": 1, \"isFinished\": false}",
                    "{\"focusId\": 0, \"page\": 1, \"isFinished\": false}",
                    "{\"focusId\": 100, \"page\": 0, \"isFinished\": false}",
                    "{\"focusId\": 100, \"page\": 1}"
            );

            for (String invalidRequest : invalidRequests) {
                mockMvc.perform(
                                post("/api/v1/focuses/end")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invalidRequest)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("COMMON-002"));
            }
        }

    }

    @Nested
    @DisplayName("최근 포커스 조회")
    class GetRecentFocuses {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            CursorResponse<FocusResponseDto.RecentFocusItem, Long> response = CursorResponse.of(
                    List.of(
                            new FocusResponseDto.RecentFocusItem(
                                    100L,
                                    20L,
                                    "첫사랑의 침공",
                                    "권혁일",
                                    "https://cdn.nook.com/covers/book.jpg",
                                    LocalDateTime.of(2026, 3, 22, 14, 0, 0),
                                    LocalDateTime.of(2026, 3, 22, 14, 34, 26),
                                    "00:34:26"
                            )
                    ),
                    null,
                    false
            );

            given(focusQueryService.getRecentFocuses(any(User.class), any(), anyInt()))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/recent")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .param("size", "10")
                    )
                    .andExpect(status().isOk())
                    .andDo(documentWithAuth(
                            "focus-controller-test/최근_포커스_조회_성공",
                            queryParameters(
                                    parameterWithName("cursor").optional().description("커서(마지막 포커스 ID). 최초 조회 시 미전달"),
                                    parameterWithName("size").description("조회할 개수 (기본값: 10, 최대: 50)")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.items[]").type(ARRAY).description("포커스 목록"),
                                            fieldWithPath("result.items[].focusId").type(NUMBER).description("포커스 ID"),
                                            fieldWithPath("result.items[].bookId").type(NUMBER).description("책 ID"),
                                            fieldWithPath("result.items[].bookTitle").type(STRING).description("책 제목"),
                                            fieldWithPath("result.items[].author").type(STRING).description("저자"),
                                            fieldWithPath("result.items[].coverImageUrl").type(STRING).description("책 커버 이미지 URL"),
                                            fieldWithPath("result.items[].startedAt").type(STRING).description("포커스 시작 시각"),
                                            fieldWithPath("result.items[].endedAt").type(STRING).description("포커스 종료 시각"),
                                            fieldWithPath("result.items[].durationText").type(STRING).description("집중 시간(HH:mm:ss)"),
                                            fieldWithPath("result.nextCursor").type(NUMBER).optional().description("다음 커서 (다음 페이지 없으면 null)"),
                                            fieldWithPath("result.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                                    )
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("포커스 홈 조회")
    class GetFocusHome {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            FocusResponseDto.HomeResponse response = new FocusResponseDto.HomeResponse(
                    "01:04:26",
                    ReadingStatus.READING,
                    CursorResponse.of(
                            List.of(new FocusResponseDto.HomeBookItem(
                                    20L,
                                    "첫사랑의 침공",
                                    "권혁일",
                                    "https://cdn.nook.com/covers/book.jpg",
                                    "00:34:26"
                            )),
                            10L,
                            true
                    )
            );
            given(focusQueryService.getFocusHome(any(User.class), eq(ReadingStatus.READING), eq(10L), eq(20)))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/home")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .param("status", "READING")
                                    .param("cursor", "10")
                                    .param("size", "20")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("SUCCESS-200"))
                    .andExpect(jsonPath("$.result.todayFocusTime").value("01:04:26"))
                    .andExpect(jsonPath("$.result.readingStatus").value("READING"))
                    .andExpect(jsonPath("$.result.books.items[0].bookId").value(20L))
                    .andExpect(jsonPath("$.result.books.items[0].title").value("첫사랑의 침공"))
                    .andExpect(jsonPath("$.result.books.items[0].author").value("권혁일"))
                    .andExpect(jsonPath("$.result.books.items[0].coverUrl").value("https://cdn.nook.com/covers/book.jpg"))
                    .andExpect(jsonPath("$.result.books.items[0].todayFocusTime").value("00:34:26"))
                    .andExpect(jsonPath("$.result.books.nextCursor").value(10L))
                    .andExpect(jsonPath("$.result.books.hasNext").value(true))
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_홈_조회_성공",
                            queryParameters(
                                    parameterWithName("status").description("독서 상태 (BEFORE, READING, FINISHED)"),
                                    parameterWithName("cursor").optional().description("커서(마지막 서재 ID). 최초 조회 시 미전달 또는 0"),
                                    parameterWithName("size").optional().description("조회할 개수 (기본값: 20, 최대: 100)")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.todayFocusTime").type(STRING).description("오늘 전체 포커스 시간(HH:mm:ss)"),
                                            fieldWithPath("result.readingStatus").type(STRING).description("조회한 독서 상태"),
                                            fieldWithPath("result.books").type(OBJECT).description("커서 기반 책 목록"),
                                            fieldWithPath("result.books.items[]").type(ARRAY).description("책 목록"),
                                            fieldWithPath("result.books.items[].bookId").type(NUMBER).description("책 ID"),
                                            fieldWithPath("result.books.items[].title").type(STRING).description("책 제목"),
                                            fieldWithPath("result.books.items[].author").type(STRING).description("저자"),
                                            fieldWithPath("result.books.items[].coverUrl").type(STRING).description("책 커버 이미지 URL"),
                                            fieldWithPath("result.books.items[].todayFocusTime").type(STRING).description("책의 오늘 포커스 시간(HH:mm:ss)"),
                                            fieldWithPath("result.books.nextCursor").type(NUMBER).optional().description("다음 커서 (다음 페이지 없으면 null)"),
                                            fieldWithPath("result.books.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                                    )
                            )
                    ));

            verify(focusQueryService).getFocusHome(any(User.class), eq(ReadingStatus.READING), eq(10L), eq(20));
        }

        @Test
        @WithCustomUser
        @DisplayName("cursor를 생략하면 첫 페이지로 조회한다")
        void treatsOmittedCursorAsFirstPage() throws Exception {
            FocusResponseDto.HomeResponse response = new FocusResponseDto.HomeResponse(
                    "00:00:00",
                    ReadingStatus.BEFORE,
                    CursorResponse.of(List.of(), null, false)
            );
            given(focusQueryService.getFocusHome(any(User.class), eq(ReadingStatus.BEFORE), isNull(), eq(20)))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/home")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .param("status", "BEFORE")
                    )
                    .andExpect(status().isOk());

            verify(focusQueryService).getFocusHome(any(User.class), eq(ReadingStatus.BEFORE), isNull(), eq(20));
        }

        @Test
        @WithCustomUser
        @DisplayName("cursor 0은 첫 페이지로 조회한다")
        void treatsZeroCursorAsFirstPage() throws Exception {
            FocusResponseDto.HomeResponse response = new FocusResponseDto.HomeResponse(
                    "00:00:00",
                    ReadingStatus.FINISHED,
                    CursorResponse.of(List.of(), null, false)
            );
            given(focusQueryService.getFocusHome(any(User.class), eq(ReadingStatus.FINISHED), isNull(), eq(20)))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/home")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .param("status", "FINISHED")
                                    .param("cursor", "0")
                    )
                    .andExpect(status().isOk());

            verify(focusQueryService).getFocusHome(any(User.class), eq(ReadingStatus.FINISHED), isNull(), eq(20));
        }

        @Test
        @WithCustomUser
        @DisplayName("양의 커서 terminal page는 null nextCursor와 false hasNext를 그대로 반환한다")
        void preservesTerminalPageMetadataForPositiveCursor() throws Exception {
            FocusResponseDto.HomeResponse response = new FocusResponseDto.HomeResponse(
                    "00:00:00",
                    ReadingStatus.FINISHED,
                    CursorResponse.of(List.of(), null, false)
            );
            given(focusQueryService.getFocusHome(any(User.class), eq(ReadingStatus.FINISHED), eq(23L), eq(7)))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/home")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                                    .param("status", "FINISHED")
                                    .param("cursor", "23")
                                    .param("size", "7")
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            {
                              "isSuccess": true,
                              "code": "SUCCESS-200",
                              "message": "요청에 성공했습니다.",
                              "result": {
                                "todayFocusTime": "00:00:00",
                                "readingStatus": "FINISHED",
                                "books": {
                                  "items": [],
                                  "nextCursor": null,
                                  "hasNext": false
                                }
                              }
                            }
                            """, true));

            verify(focusQueryService).getFocusHome(any(User.class), eq(ReadingStatus.FINISHED), eq(23L), eq(7));
        }

        @Test
        @WithCustomUser
        @DisplayName("잘못된 요청은 서비스 호출 없이 거절한다")
        void rejectsInvalidQueryParameters() throws Exception {
            List<String[]> invalidParameters = List.of(
                    new String[]{"size", "20"},
                    new String[]{"status", "UNKNOWN"},
                    new String[]{"status", "READING", "cursor", "-1"},
                    new String[]{"status", "READING", "size", "0"},
                    new String[]{"status", "READING", "size", "101"}
            );

            for (String[] parameters : invalidParameters) {
                var request = get("/api/v1/focuses/home").header(AUTH_HEADER, AUTH_TOKEN);
                for (int index = 0; index < parameters.length; index += 2) {
                    request.param(parameters[index], parameters[index + 1]);
                }

                mockMvc.perform(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("COMMON-002"));
            }

            verifyNoInteractions(focusQueryService);
        }

        @Test
        @DisplayName("인증 없이 조회하면 401을 반환한다")
        void rejectsUnauthenticatedRequest() throws Exception {
            mockMvc.perform(
                            get("/api/v1/focuses/home")
                                    .param("status", "READING")
                    )
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(focusQueryService);
        }
    }
}
