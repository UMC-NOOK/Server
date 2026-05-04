package app.nook.controller.focus;

import app.nook.focus.controller.FocusController;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.FocusService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.domain.User;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
public class FocusControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private FocusService focusService;

    @MockitoBean
    private app.nook.focus.service.ThemeService themeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithCustomUser
    @DisplayName("포커스 시작 성공")
    void 포커스_시작_성공() throws Exception {
        // given
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

        // when & then
        mockMvc.perform(
                        post("/api/v1/focuses/start")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
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

    @Test
    @WithCustomUser
    @DisplayName("포커스 종료 성공")
    void 포커스_종료_성공() throws Exception {
        // given
        FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(
                100L,
                72,
                true
        );

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

        given(focusService.endFocus(eq(1L), eq(request))).willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/v1/focuses/end")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
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