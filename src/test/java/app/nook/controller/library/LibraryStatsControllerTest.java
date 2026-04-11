package app.nook.controller.library;

import app.nook.focus.repository.FocusRepository;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.library.controller.LibraryStatsController;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.FocusTimeSlot;
import app.nook.library.service.LibraryStatsService;
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
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LibraryStatsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class LibraryStatsControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private LibraryStatsService libraryStatsService;

    @MockitoBean
    private FocusRepository focusRepository;

    @DisplayName("월별 포커스 통계 조회")
    @Nested
    class ViewMonthlyStats {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("월별 포커스 통계 조회")
            @WithCustomUser
            void viewMonthlyStats() throws Exception {
                FocusRankDto.MonthlyBooksResponseDto response =
                        new FocusRankDto.MonthlyBooksResponseDto(
                                YearMonth.of(2026, 2),
                                2,
                                List.of(
                                        new FocusRankDto.DailyBookItem(
                                                LocalDate.of(2026, 2, 1),
                                                2L,
                                                new FocusRankDto.BookCalendarInfo(101L, "https://example.com/101.jpg")
                                        )
                                )
                        );

                given(libraryStatsService.viewMonthly(any(), any(YearMonth.class)))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/stats/monthly")
                                        .param("yearMonth", "2026-02")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                queryParameters(
                                        parameterWithName("yearMonth").description("조회 대상 월 (yyyy-MM)")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.yearMonth").type(JsonFieldType.STRING).description("조회 월"),
                                        fieldWithPath("result.totalBookCount").type(JsonFieldType.NUMBER).description("월 전체 책 수"),
                                        fieldWithPath("result.days").type(JsonFieldType.ARRAY).description("일자별 통계 목록"),
                                        fieldWithPath("result.days[].date").type(JsonFieldType.STRING).description("날짜"),
                                        fieldWithPath("result.days[].bookCount").type(JsonFieldType.NUMBER).description("해당 날짜에 읽은 책 수"),
                                        fieldWithPath("result.days[].topBook").type(JsonFieldType.OBJECT).optional().description("가장 오래 읽은 책"),
                                        fieldWithPath("result.days[].topBook.bookId").type(JsonFieldType.NUMBER).optional().description("도서 ID"),
                                        fieldWithPath("result.days[].topBook.coverUrl").type(JsonFieldType.STRING).optional().description("도서 커버 URL")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("인증 정보가 없으면 401")
            void viewMonthlyStatsWithoutAuth() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/stats/monthly")
                                        .param("yearMonth", "2026-02")
                        )
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @DisplayName("월별 포커스 시간 통계 조회")
    @Nested
    class ViewFocusMonthlyStats {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @DisplayName("월별 포커스 시간 통계 조회")
            @WithCustomUser
            void viewFocusMonthlyStats() throws Exception {
                FocusRankDto.FocusBookResponseDto response =
                        new FocusRankDto.FocusBookResponseDto(
                                YearMonth.of(2026, 2),
                                95,
                                List.of(
                                        new FocusRankDto.FocusDateItem(
                                                LocalDate.of(2026, 2, 1),
                                                FocusTimeSlot.FOCUS_04
                                        ),
                                        new FocusRankDto.FocusDateItem(
                                                LocalDate.of(2026, 2, 2),
                                                FocusTimeSlot.FOCUS_02
                                        )
                                )
                        );

                given(libraryStatsService.viewFocusTimeStats(any(), any(YearMonth.class)))
                        .willReturn(response);

                mockMvc.perform(
                                get("/api/v1/library/stats/focus-monthly")
                                        .param("yearMonth", "2026-02")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "{class-name}/{method-name}",
                                queryParameters(
                                        parameterWithName("yearMonth").description("조회 대상 월 (yyyy-MM)")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.yearMonth").type(JsonFieldType.STRING).description("조회 월"),
                                        fieldWithPath("result.totalFocusMin").type(JsonFieldType.NUMBER).description("월 전체 포커스 시간(분)"),
                                        fieldWithPath("result.focusBookItems").type(JsonFieldType.ARRAY).description("일자별 포커스 강도 목록"),
                                        fieldWithPath("result.focusBookItems[].date").type(JsonFieldType.STRING).description("날짜"),
                                        fieldWithPath("result.focusBookItems[].timeSlot").type(JsonFieldType.STRING).description("포커스 강도 슬롯 (FOCUS_00~FOCUS_04)")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @DisplayName("yearMonth 형식이 잘못되면 400")
            @WithCustomUser
            void invalidYearMonthFormat() throws Exception {
                mockMvc.perform(
                                get("/api/v1/library/stats/focus-monthly")
                                        .param("yearMonth", "2026/02")
                                        .header(AUTH_HEADER, AUTH_TOKEN)
                        )
                        .andExpect(status().isBadRequest());
            }
        }
    }
}
