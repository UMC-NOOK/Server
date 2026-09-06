package app.nook.controller.timeline;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.timeline.controller.TimelineController;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.timeline.dto.TimelineCursor;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.service.TimelineQueryService;
import app.nook.timeline.util.TimelineCursorCodec;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TimelineController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class TimelineControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private TimelineQueryService timelineQueryService;

    @DisplayName("독서 이력 요약 조회")
    @Nested
    class GetTimelineSummary {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("독서 이력 요약 조회")
            @WithCustomUser
            void getTimelineSummary() throws Exception {
                TimelineResponseDto.TimelineItemDto previewItem =
                        new TimelineResponseDto.TimelineItemDto(
                                10L,
                                TimelineType.REGISTER,
                                LocalDateTime.of(2025, 12, 30, 12, 0),
                                "서재에 등록했어요",
                                null,
                                "서재에 등록했어요",
                                12L
                        );

                TimelineResponseDto.TimelineDateGroupDto group =
                        new TimelineResponseDto.TimelineDateGroupDto(
                                2025,
                                "12.30",
                                true,
                                List.of(previewItem)
                        );

                TimelineResponseDto.TimelineSummaryDto response =
                        new TimelineResponseDto.TimelineSummaryDto(
                                12L,
                                new TimelineResponseDto.FocusSummaryDto(
                                        LocalDate.of(2025, 12, 30),
                                        LocalDate.of(2026, 1, 19),
                                        12262L,
                                        39,
                                        99
                                ),
                                new TimelineResponseDto.RecordSummaryDto(
                                        0,
                                        null
                                ),
                                new TimelineResponseDto.TimelinePreviewDto(
                                        List.of(group)
                                )
                        );

                given(timelineQueryService.getTimelineSummary(any(), anyLong()))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/{libraryId}/timeline/summary", 12L)
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("result.focusSummary.startedAt").value("2025.12.30"))
                        .andExpect(jsonPath("result.focusSummary.endedAt").value("2026.01.19"))
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                pathParameters(
                                        parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.libraryId").type(JsonFieldType.NUMBER).description("서재 ID"),
                                        fieldWithPath("result.focusSummary").type(JsonFieldType.OBJECT).description("포커스 요약 정보"),
                                        fieldWithPath("result.focusSummary.startedAt").type(JsonFieldType.STRING).optional().description("독서 시작일 (yyyy.MM.dd)"),
                                        fieldWithPath("result.focusSummary.endedAt").type(JsonFieldType.STRING).optional().description("독서 종료일 (yyyy.MM.dd)"),
                                        fieldWithPath("result.focusSummary.totalFocusSec").type(JsonFieldType.NUMBER).description("누적 포커스 시간(초)"),
                                        fieldWithPath("result.focusSummary.focusCount").type(JsonFieldType.NUMBER).description("포커스 횟수"),
                                        fieldWithPath("result.focusSummary.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                        fieldWithPath("result.recordSummary").type(JsonFieldType.OBJECT).description("기록 요약 정보"),
                                        fieldWithPath("result.recordSummary.recordCount").type(JsonFieldType.NUMBER).description("기록 개수"),
                                        fieldWithPath("result.recordSummary.latestRecordPreview").type(JsonFieldType.NULL).optional().description("최근 기록 preview"),
                                        fieldWithPath("result.timelinePreview").type(JsonFieldType.OBJECT).description("독서 이력 preview"),
                                        fieldWithPath("result.timelinePreview.dateGroups").type(JsonFieldType.ARRAY).description("날짜별 독서 이력 preview 그룹"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].year").type(JsonFieldType.NUMBER).description("연도"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].monthDay").type(JsonFieldType.STRING).description("월/일"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].showYear").type(JsonFieldType.BOOLEAN).description("연도 표시 여부"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items").type(JsonFieldType.ARRAY).description("해당 날짜의 독서 이력 아이템 목록"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].timelineId").type(JsonFieldType.NUMBER).description("타임라인 ID"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].type").type(JsonFieldType.STRING).description("타임라인 타입"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].occurredAt").type(JsonFieldType.STRING).description("이벤트 발생 시각"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].title").type(JsonFieldType.STRING).description("카드 제목"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].subtitle").type(JsonFieldType.NULL).optional().description("카드 부제목"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].previewText").type(JsonFieldType.STRING).description("카드 preview 텍스트"),
                                        fieldWithPath("result.timelinePreview.dateGroups[].items[].targetId").type(JsonFieldType.NUMBER).description("원본 대상 ID")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("인증 정보가 없으면 401")
            void getTimelineSummaryWithoutAuth() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/{libraryId}/timeline/summary", 12L)
                        )
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @DisplayName("독서 이력 preview 조회")
    @Nested
    class GetTimelinePreview {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("공백 커서는 첫 페이지로 조회한다")
            @WithCustomUser
            void getTimelinePreview_공백커서() throws Exception {
                given(timelineQueryService.getTimelinePreview(any(), eq(12L), isNull(), eq(20)))
                        .willReturn(new TimelineResponseDto.TimelinePageDto(List.of(), null, false));
                mockMvc.perform(get("/api/v1/library/{libraryId}/timeline", 12L)
                                .header(AUTH_HEADER, AUTH_TOKEN).param("cursor", "  "))
                        .andExpect(status().isOk());
                verify(timelineQueryService).getTimelinePreview(any(), eq(12L), isNull(), eq(20));
            }

            @Test
            @WithCustomUser
            @DisplayName("커서와 조회 크기를 서비스에 전달한다")
            void getTimelinePreview_커서전달() throws Exception {
                TimelineCursor cursor = new TimelineCursor(LocalDateTime.parse("2026-09-06T12:34:56.123456"), 103L);
                given(timelineQueryService.getTimelinePreview(any(), eq(12L), eq(cursor), eq(2)))
                        .willReturn(new TimelineResponseDto.TimelinePageDto(List.of(), null, false));
                mockMvc.perform(get("/api/v1/library/{libraryId}/timeline", 12L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .param("cursor", TimelineCursorCodec.encode(cursor)).param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.hasNext").value(false));
                verify(timelineQueryService).getTimelinePreview(any(), eq(12L), eq(cursor), eq(2));
            }

            @Test
            @DisplayName("독서 이력 preview 조회")
            @WithCustomUser
            void getTimelinePreview() throws Exception {
                TimelineResponseDto.TimelineItemDto item =
                        new TimelineResponseDto.TimelineItemDto(
                                10L,
                                TimelineType.REGISTER,
                                LocalDateTime.of(2025, 12, 30, 12, 0),
                                "서재에 등록했어요",
                                null,
                                "서재에 등록했어요",
                                12L
                        );

                TimelineResponseDto.TimelineDateGroupDto group =
                        new TimelineResponseDto.TimelineDateGroupDto(
                                2025,
                                "12.30",
                                true,
                                List.of(item)
                        );

                TimelineResponseDto.TimelinePageDto response =
                        new TimelineResponseDto.TimelinePageDto(List.of(group), null, false);

                given(timelineQueryService.getTimelinePreview(any(), eq(12L), isNull(), eq(20)))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/{libraryId}/timeline", 12L)
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                pathParameters(
                                        parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID")
                                ),
                                queryParameters(
                                        parameterWithName("cursor").optional().description("이전 응답의 nextCursor, 생략하면 첫 페이지"),
                                        parameterWithName("size").optional().description("조회할 항목 수, 기본 20, 범위 1~100")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.nextCursor").type(JsonFieldType.STRING).optional().description("다음 페이지 커서, 마지막 페이지는 null"),
                                        fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                        fieldWithPath("result.dateGroups").type(JsonFieldType.ARRAY).description("날짜별 독서 이력 그룹"),
                                        fieldWithPath("result.dateGroups[].year").type(JsonFieldType.NUMBER).description("연도"),
                                        fieldWithPath("result.dateGroups[].monthDay").type(JsonFieldType.STRING).description("월/일"),
                                        fieldWithPath("result.dateGroups[].showYear").type(JsonFieldType.BOOLEAN).description("연도 표시 여부"),
                                        fieldWithPath("result.dateGroups[].items").type(JsonFieldType.ARRAY).description("해당 날짜의 독서 이력 아이템 목록"),
                                        fieldWithPath("result.dateGroups[].items[].timelineId").type(JsonFieldType.NUMBER).description("타임라인 ID"),
                                        fieldWithPath("result.dateGroups[].items[].type").type(JsonFieldType.STRING).description("타임라인 타입"),
                                        fieldWithPath("result.dateGroups[].items[].occurredAt").type(JsonFieldType.STRING).description("이벤트 발생 시각"),
                                        fieldWithPath("result.dateGroups[].items[].title").type(JsonFieldType.STRING).description("카드 제목"),
                                        fieldWithPath("result.dateGroups[].items[].subtitle").type(JsonFieldType.NULL).optional().description("카드 부제목"),
                                        fieldWithPath("result.dateGroups[].items[].previewText").type(JsonFieldType.STRING).description("카드 preview 텍스트"),
                                        fieldWithPath("result.dateGroups[].items[].targetId").type(JsonFieldType.NUMBER).description("원본 대상 ID")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @ParameterizedTest
            @ValueSource(strings = {"2026-09-06T12:00", "bad-date|1", "2026-02-30T12:00|1",
                    "2026-09-06T12:00|0", "2026-09-06T12:00|-1", "2026-09-06T12:00|abc",
                    "2026-09-06T12:00|9223372036854775808", "2026-09-06T12:00|1|extra"})
            @DisplayName("커서 내부의 날짜와 ID 및 필드 구성이 잘못되면 400")
            @WithCustomUser
            void getTimelinePreview_커서내부값오류(String payload) throws Exception {
                String cursor = Base64.getUrlEncoder()
                        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
                mockMvc.perform(get("/api/v1/library/{libraryId}/timeline", 12L)
                                .header(AUTH_HEADER, AUTH_TOKEN).param("cursor", cursor))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("COMMON-002"));
            }

            @ParameterizedTest
            @ValueSource(strings = {"0", "101"})
            @WithCustomUser
            @DisplayName("조회 크기가 범위를 벗어나면 400")
            void getTimelinePreview_크기범위오류(String size) throws Exception {
                mockMvc.perform(get("/api/v1/library/{libraryId}/timeline", 12L)
                                .header(AUTH_HEADER, AUTH_TOKEN).param("size", size))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithCustomUser
            @DisplayName("커서 형식이 잘못되면 400")
            void getTimelinePreview_커서형식오류() throws Exception {
                mockMvc.perform(get("/api/v1/library/{libraryId}/timeline", 12L)
                                .header(AUTH_HEADER, AUTH_TOKEN).param("cursor", "%%%"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("COMMON-002"));
            }

            @Test
            @DisplayName("인증 정보가 없으면 401")
            void invalidSize() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/{libraryId}/timeline", 12L)
                        )
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @DisplayName("독서 이력 상세 조회")
    @Nested
    class GetTimelineDetail {

        @Test
        @DisplayName("독서 이력 상세 조회")
        @WithCustomUser
        void getTimelineDetail() throws Exception {
            TimelineResponseDto.TimelineDetailDto response =
                    new TimelineResponseDto.TimelineDetailDto(
                            31L,
                            9001L,
                            TimelineType.RECORD,
                            LocalDateTime.of(2025, 12, 20, 21, 10),
                            new TimelineResponseDto.TimelineRecordDetailDto(
                                    "말하기와 듣기...",
                                    "FUN",
                                    List.of("https://img/a", "https://img/b")
                            )
                    );

            given(timelineQueryService.getTimelineDetail(any(), anyLong(), anyLong()))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/library/{libraryId}/timeline/{timelineId}", 12L, 31L)
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                    )
                    .andExpect(status().isOk())
                    .andDo(documentWithAuth(
                            "{class-name}/{method-name}",
                            pathParameters(
                                    parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID"),
                                    parameterWithName("timelineId").description("상세 조회할 타임라인 ID")
                            ),
                            responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result.targetId").type(JsonFieldType.NUMBER).description("원본 ID: RECORD는 recordId, FOCUS는 focusId, REGISTER/STATUS는 libraryId"),
                                    fieldWithPath("result.timelineId").type(JsonFieldType.NUMBER).description("타임라인 ID"),
                                    fieldWithPath("result.type").type(JsonFieldType.STRING).description("타임라인 타입"),
                                    fieldWithPath("result.occurredAt").type(JsonFieldType.STRING).description("이벤트 발생 시각"),
                                    fieldWithPath("result.detail").type(JsonFieldType.OBJECT).description("타입별 상세 정보"),
                                    fieldWithPath("result.detail.title").type(JsonFieldType.STRING).optional().description("STATUS 타입 상세 제목"),
                                    fieldWithPath("result.detail.description").type(JsonFieldType.STRING).optional().description("REGISTER/STATUS 타입 상세 설명"),
                                    fieldWithPath("result.detail.timeText").type(JsonFieldType.STRING).optional().description("FOCUS 타입 상세 시간 정보 (1~59초는 '1분 미만')"),
                                    fieldWithPath("result.detail.page").type(JsonFieldType.NUMBER).optional().description("FOCUS 타입 상세 종료 시점 페이지"),
                                    fieldWithPath("result.detail.content").type(JsonFieldType.STRING).optional().description("RECORD 타입 상세 기록 본문"),
                                    fieldWithPath("result.detail.emotion").type(JsonFieldType.STRING).optional().description("RECORD 타입 상세 기록 감정 코드"),
                                    fieldWithPath("result.detail.imgUrls").type(JsonFieldType.ARRAY).optional().description("RECORD 타입 상세 기록 이미지 URL 목록")
                            ))
                    ));
        }

        @Test
        @DisplayName("1분 미만 FOCUS 상세 응답 예시")
        @WithCustomUser
        void getFocusTimelineDetail() throws Exception {
            TimelineResponseDto.TimelineDetailDto response =
                    new TimelineResponseDto.TimelineDetailDto(
                            30L,
                            7001L,
                            TimelineType.FOCUS,
                            LocalDateTime.of(2026, 8, 28, 18, 25, 53),
                            new TimelineResponseDto.TimelineFocusDetailDto(
                                    "18:25 - 18:26 (1분 미만)",
                                    121
                            )
                    );

            given(timelineQueryService.getTimelineDetail(any(), anyLong(), anyLong()))
                    .willReturn(response);

            mockMvc.perform(
                            get("/api/v1/library/{libraryId}/timeline/{timelineId}", 12L, 30L)
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("result.detail.timeText").value("18:25 - 18:26 (1분 미만)"))
                    .andDo(documentWithAuth(
                            "get-timeline-detail/get-focus-timeline-detail",
                            pathParameters(
                                    parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID"),
                                    parameterWithName("timelineId").description("상세 조회할 타임라인 ID")
                            )
                    ));
        }
    }
}
