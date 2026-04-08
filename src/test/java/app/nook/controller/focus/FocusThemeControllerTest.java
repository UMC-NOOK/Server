package app.nook.controller.focus;

import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.ThemeService;
import app.nook.global.common.AbstractRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FocusThemeControllerTest extends AbstractRestDocsTests {

    @MockitoBean
    private ThemeService themeService;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUpUser() {
        User user = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        userDetails = new CustomUserDetails(user);
    }

    @Test
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
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
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
