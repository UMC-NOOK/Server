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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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
}
