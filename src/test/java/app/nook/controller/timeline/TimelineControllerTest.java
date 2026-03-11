package app.nook.controller.timeline;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.timeline.controller.TimelineController;
import app.nook.timeline.domain.enums.BookTimeLineType;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.service.TimelineQueryService;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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
                                BookTimeLineType.REGISTER,
                                LocalDateTime.of(2025, 12, 30, 12, 0),
                                new TimelineResponseDto.TimelineDisplayDateDto(2025, "12.30", true),
                                "서재에 등록했어요",
                                null,
                                "서재에 등록했어요",
                                12L,
                                List.of(TimelineResponseDto.TimelineActionType.REMOVE_FROM_LIBRARY)
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
                                        List.of(previewItem),
                                        null,
                                        false
                                )
                        );

                given(timelineQueryService.getTimelineSummary(any(), anyLong()))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/library/{libraryId}/timeline/summary", 12L)
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                pathParameters(
                                        parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.libraryId").type(JsonFieldType.NUMBER).description("서재 ID"),
                                        fieldWithPath("result.focusSummary").type(JsonFieldType.OBJECT).description("포커스 요약 정보"),
                                        fieldWithPath("result.focusSummary.startedAt").type(JsonFieldType.STRING).optional().description("독서 시작일"),
                                        fieldWithPath("result.focusSummary.endedAt").type(JsonFieldType.STRING).optional().description("독서 종료일"),
                                        fieldWithPath("result.focusSummary.totalFocusSec").type(JsonFieldType.NUMBER).description("누적 포커스 시간(초)"),
                                        fieldWithPath("result.focusSummary.focusCount").type(JsonFieldType.NUMBER).description("포커스 횟수"),
                                        fieldWithPath("result.focusSummary.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                        fieldWithPath("result.recordSummary").type(JsonFieldType.OBJECT).description("기록 요약 정보"),
                                        fieldWithPath("result.recordSummary.recordCount").type(JsonFieldType.NUMBER).description("기록 개수"),
                                        fieldWithPath("result.recordSummary.latestRecordPreview").type(JsonFieldType.NULL).optional().description("최근 기록 preview"),
                                        fieldWithPath("result.timelinePreview").type(JsonFieldType.OBJECT).description("독서 이력 preview"),
                                        fieldWithPath("result.timelinePreview.items").type(JsonFieldType.ARRAY).description("독서 이력 아이템 목록"),
                                        fieldWithPath("result.timelinePreview.items[].timelineId").type(JsonFieldType.NUMBER).description("타임라인 ID"),
                                        fieldWithPath("result.timelinePreview.items[].type").type(JsonFieldType.STRING).description("타임라인 타입"),
                                        fieldWithPath("result.timelinePreview.items[].occurredAt").type(JsonFieldType.STRING).description("이벤트 발생 시각"),
                                        fieldWithPath("result.timelinePreview.items[].displayDate").type(JsonFieldType.OBJECT).description("화면 표시용 날짜 정보"),
                                        fieldWithPath("result.timelinePreview.items[].displayDate.year").type(JsonFieldType.NUMBER).description("표시 연도"),
                                        fieldWithPath("result.timelinePreview.items[].displayDate.monthDay").type(JsonFieldType.STRING).description("월/일 표시값"),
                                        fieldWithPath("result.timelinePreview.items[].displayDate.showYear").type(JsonFieldType.BOOLEAN).description("연도 표시 여부"),
                                        fieldWithPath("result.timelinePreview.items[].title").type(JsonFieldType.STRING).description("카드 제목"),
                                        fieldWithPath("result.timelinePreview.items[].subtitle").type(JsonFieldType.NULL).optional().description("카드 부제목"),
                                        fieldWithPath("result.timelinePreview.items[].previewText").type(JsonFieldType.STRING).description("카드 preview 텍스트"),
                                        fieldWithPath("result.timelinePreview.items[].targetId").type(JsonFieldType.NUMBER).description("원본 대상 ID"),
                                        fieldWithPath("result.timelinePreview.items[].actions").type(JsonFieldType.ARRAY).description("지원 액션 목록"),
                                        fieldWithPath("result.timelinePreview.items[].actions[]").type(JsonFieldType.STRING).description("액션 타입"),
                                        fieldWithPath("result.timelinePreview.nextCursor").type(JsonFieldType.NULL).optional().description("다음 커서"),
                                        fieldWithPath("result.timelinePreview.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
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
                                get("/api/library/{libraryId}/timeline/summary", 12L)
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
            @DisplayName("독서 이력 preview 조회")
            @WithCustomUser
            void getTimelinePreview() throws Exception {
                TimelineResponseDto.TimelineItemDto item =
                        new TimelineResponseDto.TimelineItemDto(
                                10L,
                                BookTimeLineType.REGISTER,
                                LocalDateTime.of(2025, 12, 30, 12, 0),
                                new TimelineResponseDto.TimelineDisplayDateDto(2025, "12.30", true),
                                "서재에 등록했어요",
                                null,
                                "서재에 등록했어요",
                                12L,
                                List.of(TimelineResponseDto.TimelineActionType.REMOVE_FROM_LIBRARY)
                        );

                TimelineResponseDto.TimelinePreviewDto response =
                        new TimelineResponseDto.TimelinePreviewDto(
                                List.of(item),
                                10L,
                                true
                        );

                given(timelineQueryService.getTimelinePreview(any(), anyLong(), any(), anyInt()))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/library/{libraryId}/timeline", 12L)
                                        .param("cursor", "20")
                                        .param("size", "10")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                pathParameters(
                                        parameterWithName("libraryId").description("독서 이력을 조회할 서재 ID")
                                ),
                                queryParameters(
                                        parameterWithName("cursor").optional().description("커서(이전 페이지 마지막 timeline ID)"),
                                        parameterWithName("size").description("조회할 개수")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.items").type(JsonFieldType.ARRAY).description("독서 이력 아이템 목록"),
                                        fieldWithPath("result.items[].timelineId").type(JsonFieldType.NUMBER).description("타임라인 ID"),
                                        fieldWithPath("result.items[].type").type(JsonFieldType.STRING).description("타임라인 타입"),
                                        fieldWithPath("result.items[].occurredAt").type(JsonFieldType.STRING).description("이벤트 발생 시각"),
                                        fieldWithPath("result.items[].displayDate").type(JsonFieldType.OBJECT).description("화면 표시용 날짜 정보"),
                                        fieldWithPath("result.items[].displayDate.year").type(JsonFieldType.NUMBER).description("표시 연도"),
                                        fieldWithPath("result.items[].displayDate.monthDay").type(JsonFieldType.STRING).description("월/일 표시값"),
                                        fieldWithPath("result.items[].displayDate.showYear").type(JsonFieldType.BOOLEAN).description("연도 표시 여부"),
                                        fieldWithPath("result.items[].title").type(JsonFieldType.STRING).description("카드 제목"),
                                        fieldWithPath("result.items[].subtitle").type(JsonFieldType.NULL).optional().description("카드 부제목"),
                                        fieldWithPath("result.items[].previewText").type(JsonFieldType.STRING).description("카드 preview 텍스트"),
                                        fieldWithPath("result.items[].targetId").type(JsonFieldType.NUMBER).description("원본 대상 ID"),
                                        fieldWithPath("result.items[].actions").type(JsonFieldType.ARRAY).description("지원 액션 목록"),
                                        fieldWithPath("result.items[].actions[]").type(JsonFieldType.STRING).description("액션 타입"),
                                        fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).description("다음 커서"),
                                        fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("size가 범위를 벗어나면 400")
            @WithCustomUser
            void invalidSize() throws Exception {
                mockMvc.perform(
                                get("/api/library/{libraryId}/timeline", 12L)
                                        .param("size", "101")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isBadRequest());
            }
        }
    }
}
