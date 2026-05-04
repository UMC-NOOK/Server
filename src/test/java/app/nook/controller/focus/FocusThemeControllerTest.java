package app.nook.controller.focus;

import app.nook.focus.controller.FocusController;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.ThemeService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
public class FocusThemeControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private ThemeService themeService;

    @MockitoBean
    private app.nook.focus.service.FocusService focusService;

    @Test
    @WithCustomUser
    @DisplayName("포커스 테마 목록 조회 성공")
    void 포커스_테마목록_조회_성공() throws Exception {
        // given
        FocusResponseDto.ThemeListDto response = new FocusResponseDto.ThemeListDto(
                List.of(
                        new FocusResponseDto.ThemeItemDto(1L, "Theme1", "https://cdn.nook.com/themes/theme1.png"),
                        new FocusResponseDto.ThemeItemDto(2L, "Theme2", "https://cdn.nook.com/themes/theme2.png"),
                        new FocusResponseDto.ThemeItemDto(3L, "Theme3", "https://cdn.nook.com/themes/theme3.png"),
                        new FocusResponseDto.ThemeItemDto(4L, "NONE",   "https://cdn.nook.com/themes/none.png")
                )
        );

        given(themeService.getThemes()).willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/focuses/themes")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
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
