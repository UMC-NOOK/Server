package app.nook.controller.admin;

import app.nook.admin.AdminAccessChecker;
import app.nook.admin.controller.AdminImageController;
import app.nook.admin.dto.OrphanScanResult;
import app.nook.admin.service.OrphanImageService;
import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.service.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminImageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class AdminImageControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private AdminAccessChecker adminAccessChecker;

    @MockitoBean
    private OrphanImageService orphanImageService;

    private CustomUserDetails userDetails() {
        User user = User.builder()
                .email("someone@test.com")
                .nickName("someone")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        return new CustomUserDetails(user);
    }

    @Test
    void 고아이미지_대조_관리자_성공() throws Exception {
        given(orphanImageService.scan())
                .willReturn(new OrphanScanResult(10, 2, List.of("record/users/1/a.jpg"), false));

        mockMvc.perform(
                        get("/api/v1/admin/images/orphans")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(user(userDetails()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void 고아이미지_대조_비관리자_403() throws Exception {
        willThrow(new CustomException(AuthErrorCode.PERMISSION_DENIED))
                .given(adminAccessChecker).verifyAdmin(any());

        mockMvc.perform(
                        get("/api/v1/admin/images/orphans")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .with(user(userDetails()))
                )
                .andExpect(status().isForbidden());
    }
}
