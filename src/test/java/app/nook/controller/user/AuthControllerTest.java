package app.nook.controller.user;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.global.config.WebSecurityConfig;
import app.nook.user.controller.AuthController;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.oauth.OAuthService;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.service.CustomUserDetails;
import app.nook.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class AuthControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private OAuthService oAuthService;

    @MockitoBean
    private UserService userService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 소셜로그인_성공() throws Exception {
        // given
        OAuthDTO.OAuthLoginRequest request =
                new OAuthDTO.OAuthLoginRequest("GOOGLE","auth-code");

        UserDTO.LoginResponse response =
                UserDTO.LoginResponse.builder()
                        .id(1L)
                        .email("jiwon@kakao.com")
                        .nickName("jiwon")
                        .accessToken("test-access-token")
                        .refreshToken("test-refresh-token")
                        .build();

        given(oAuthService.login(any(), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/oauth")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("code")
                                        .description("OAuth 인가 코드"),
                                fieldWithPath("provider")
                                        .description("OAuth 제공자 (google, kakao)")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.id").description("사용자ID"),
                                        fieldWithPath("result.email").description("이메일"),
                                        fieldWithPath("result.nickName").description("닉네임"),
                                        fieldWithPath("result.accessToken").description("엑세스 토큰"),
                                        fieldWithPath("result.refreshToken").description("리프레시 토큰")
                                )
                        )
                ));
    }

    @Test
    void DEV로그인_성공() throws Exception {
        // given
        UserDTO.DevLoginRequest request =
                new UserDTO.DevLoginRequest("dev@test.com","DEV_USER");

        UserDTO.LoginResponse response =
                UserDTO.LoginResponse.builder()
                        .id(1L)
                        .email("dev@test.com")
                        .nickName("DEV_USER")
                        .accessToken("test-access-token")
                        .refreshToken("test-refresh-token")
                        .build();

        given(userService.devLogin(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/dev/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("email")
                                        .description("DEV 유저 이메일"),
                                fieldWithPath("nickName")
                                        .description("DEV 유저 닉네임")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.id").description("사용자ID"),
                                        fieldWithPath("result.email").description("이메일"),
                                        fieldWithPath("result.nickName").description("닉네임"),
                                        fieldWithPath("result.accessToken").description("엑세스 토큰"),
                                        fieldWithPath("result.refreshToken").description("리프레시 토큰")
                                )
                        )
                ));
    }

    @Test
    void DEV회원가입_성공() throws Exception {
        UserDTO.DevSignUpRequest request =
                new UserDTO.DevSignUpRequest("new@test.com", "NEW_USER");

        UserDTO.LoginResponse response =
                UserDTO.LoginResponse.builder()
                        .id(3L)
                        .email("new@test.com")
                        .nickName("NEW_USER")
                        .build();

        given(userService.devSignUp(any()))
                .willReturn(response);

                mockMvc.perform(
                        post("/api/v1/auth/dev/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("email").description("DEV 유저 이메일"),
                                fieldWithPath("nickName").description("DEV 유저 닉네임")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.id").description("사용자ID"),
                                        fieldWithPath("result.email").description("이메일"),
                                        fieldWithPath("result.nickName").description("닉네임")
                                )
                        )
                ));
    }

    @Test
    void 소셜로그인_실패_잘못된_프로바이더() throws Exception {
        OAuthDTO.OAuthLoginRequest request =
                new OAuthDTO.OAuthLoginRequest("INVALID","auth-code");

        given(oAuthService.login(any(), any()))
                .willThrow(new CustomException(AuthErrorCode.INVALID_OAUTH_PROVIDER));

        mockMvc.perform(
                        post("/api/v1/auth/oauth")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void DEV로그인_실패_유저없음() throws Exception {
        UserDTO.DevLoginRequest request =
                new UserDTO.DevLoginRequest("missing@test.com","DEV_USER");

        given(userService.devLogin(any()))
                .willThrow(new CustomException(AuthErrorCode.USER_NOT_FOUND));

        mockMvc.perform(
                        post("/api/v1/auth/dev/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void DEV회원가입_실패_이메일중복() throws Exception {
        UserDTO.DevSignUpRequest request =
                new UserDTO.DevSignUpRequest("dup@test.com", "DUP_USER");

        given(userService.devSignUp(any()))
                .willThrow(new CustomException(AuthErrorCode.EMAIL_DUPLICATE));

        mockMvc.perform(
                        post("/api/v1/auth/dev/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void 토큰_재발급_성공() throws Exception {
        UserDTO.TokenReissueRequest request = new UserDTO.TokenReissueRequest("valid-refresh-token");
        UserDTO.TokenReissueResponse response = new UserDTO.TokenReissueResponse("new-access-token", "new-refresh-token");

        given(userService.reissueAccessToken(any()))
                .willReturn(response);

        mockMvc.perform(
                        post("/api/v1/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("refreshToken").description("새 access token 발급에 사용할 refresh token")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.accessToken").description("재발급된 access token"),
                                        fieldWithPath("result.refreshToken").description("재발급된 refresh token")
                                )
                        )
                ));
    }

    @Test
    void 토큰_재발급_실패_만료된_리프레시토큰() throws Exception {
        UserDTO.TokenReissueRequest request = new UserDTO.TokenReissueRequest("expired-refresh-token");

        given(userService.reissueAccessToken(any()))
                .willThrow(new CustomException(AuthErrorCode.TOKEN_EXPIRED));

        mockMvc.perform(
                        post("/api/v1/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 내정보_조회_성공() throws Exception {
        // given
        User user = User.builder()
                .email("jiwon@kakao.com")
                .nickName("jiwon")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        UserDTO.UserInfo response = new UserDTO.UserInfo(
                2L,
                "jiwon@kakao.com",
                "jiwon"
        );

        given(userService.getThisUser(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.id").description("사용자ID"),
                                        fieldWithPath("result.email").description("이메일"),
                                        fieldWithPath("result.nickName").description("닉네임")
                                )
                        )
                ));
    }

}
