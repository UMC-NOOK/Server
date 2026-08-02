package app.nook.controller.user;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.config.WebSecurityConfig;
import app.nook.user.controller.DevAuthController;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.WithdrawService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DevAuthController 는 @Profile({"local","dev"}) 이므로 dev 프로파일을 추가로 활성화해 빈을 로드
 */
@WebMvcTest(
        controllers = DevAuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
@ActiveProfiles({"test", "dev"})
class DevAuthControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private WithdrawService withdrawService;

    private CustomUserDetails userDetails() {
        User user = User.builder()
                .email("dev@test.com")
                .nickName("dev")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        return new CustomUserDetails(user);
    }

    @Test
    void 개발용_즉시_완전탈퇴_성공() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/auth/dev/withdraw")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(user(userDetails()))
                )
                .andExpect(status().isOk());

        then(withdrawService).should().hardDelete(any(User.class));
    }
}
