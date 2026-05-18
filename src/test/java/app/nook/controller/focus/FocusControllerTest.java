package app.nook.controller.focus;

import app.nook.focus.controller.FocusController;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.FocusQueryService;
import app.nook.focus.service.FocusService;
import app.nook.focus.service.ThemeService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.dto.CursorResponse;
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
    private ThemeService themeService;

    @MockitoBean
    private FocusQueryService focusQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("테마 목록 조회")
    class GetThemes {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            FocusResponseDto.ThemeListDto response = new FocusResponseDto.ThemeListDto(
                    List.of(
                            new FocusResponseDto.ThemeItemDto(1L, "THEME1", "https://cdn.nook.com/themes/theme1.png"),
                            new FocusResponseDto.ThemeItemDto(2L, "THEME2", "https://cdn.nook.com/themes/theme2.png"),
                            new FocusResponseDto.ThemeItemDto(3L, "THEME3", "https://cdn.nook.com/themes/theme3.png"),
                            new FocusResponseDto.ThemeItemDto(4L, "NONE", "https://cdn.nook.com/themes/none.png")
                    )
            );

            given(themeService.getThemes()).willReturn(response);

            mockMvc.perform(
                            get("/api/v1/focuses/themes")
                                    .header(AUTH_HEADER, AUTH_TOKEN)
                    )
                    .andExpect(status().isOk())
                    .andDo(documentWithAuth(
                            "focus-controller-test/테마_목록_조회_성공",
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.themes[]").description("테마 목록"),
                                            fieldWithPath("result.themes[].themeId").description("테마 ID"),
                                            fieldWithPath("result.themes[].name").description("테마 이름(ENUM 문자열)"),
                                            fieldWithPath("result.themes[].imageUrl").description("테마 이미지 URL")
                                    )
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("포커스 시작")
    class StartFocus {

        @Test
        @WithCustomUser
        @DisplayName("성공")
        void 성공() throws Exception {
            FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(10L, 1L);

            FocusResponseDto.FocusStart response = new FocusResponseDto.FocusStart(
                    100L,
                    10L,
                    20L,
                    "첫사랑의 침공",
                    "권혁일",
                    1L,
                    "THEME1",
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
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_시작_성공",
                            requestFields(
                                    fieldWithPath("libraryId").description("서재 ID"),
                                    fieldWithPath("themeId").description("선택한 테마 ID")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.focusId").description("포커스 ID"),
                                            fieldWithPath("result.libraryId").description("서재 ID"),
                                            fieldWithPath("result.bookId").description("책 ID"),
                                            fieldWithPath("result.bookTitle").description("책 제목"),
                                            fieldWithPath("result.author").description("저자"),
                                            fieldWithPath("result.themeId").description("테마 ID"),
                                            fieldWithPath("result.themeName").description("테마 이름"),
                                            fieldWithPath("result.startedAt").description("포커스 시작 시각")
                                    )
                            )
                    ));
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
                    10L,
                    LocalDateTime.of(2026, 3, 22, 14, 0, 0),
                    LocalDateTime.of(2026, 3, 22, 14, 34, 26),
                    2066,
                    "00:34:26",
                    72,
                    5666L,
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
                    .andDo(documentWithAuth(
                            "focus-controller-test/포커스_종료_성공",
                            requestFields(
                                    fieldWithPath("focusId").description("종료할 포커스 ID"),
                                    fieldWithPath("page").description("현재까지 읽은 페이지"),
                                    fieldWithPath("isFinished").description("완독 여부")
                            ),
                            responseFields(
                                    ApiResponseSnippet.withResult(
                                            fieldWithPath("result.focusId").description("포커스 ID"),
                                            fieldWithPath("result.libraryId").description("서재 ID"),
                                            fieldWithPath("result.startedAt").description("포커스 시작 시각"),
                                            fieldWithPath("result.endedAt").description("포커스 종료 시각"),
                                            fieldWithPath("result.durationSec").description("집중 시간(초)"),
                                            fieldWithPath("result.durationText").description("집중 시간(HH:mm:ss)"),
                                            fieldWithPath("result.page").description("현재까지 읽은 페이지"),
                                            fieldWithPath("result.totalFocusSec").description("누적 포커스 시간(초)"),
                                            fieldWithPath("result.readingStatus").description("독서 상태")
                                    )
                            )
                    ));
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
