package app.nook.controller.user;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.response.AuthErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.user.controller.UserController;
import app.nook.user.dto.UserProfileDto;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class UserControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private UserProfileService userProfileService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users/me
    // ─────────────────────────────────────────────────────────

    @Test
    @WithCustomUser
    @DisplayName("마이페이지 내정보 조회 성공")
    void 내정보_조회_성공() throws Exception {
        UserProfileDto.MyPageResponse response = new UserProfileDto.MyPageResponse(
                1L,
                "jiwon",
                "jiwon@kakao.com",
                "https://presigned-url.example.com/profile.png"
        );

        given(userProfileService.getMyPage(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(1L))
                .andExpect(jsonPath("$.result.nickName").value("jiwon"))
                .andExpect(jsonPath("$.result.email").value("jiwon@kakao.com"))
                .andExpect(jsonPath("$.result.profileImageUrl").exists())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.userId").description("사용자 ID"),
                                        fieldWithPath("result.nickName").description("닉네임"),
                                        fieldWithPath("result.email").description("이메일"),
                                        fieldWithPath("result.profileImageUrl").description("프로필 이미지 CDN URL (없으면 null)")
                                )
                        )
                ));
    }

    @Test
    @DisplayName("인증 없음 - 내정보 조회 401")
    void 인증없음_내정보조회_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/users/me/nickname
    // ─────────────────────────────────────────────────────────

    @Test
    @WithCustomUser
    @DisplayName("닉네임 수정 성공")
    void 닉네임_수정_성공() throws Exception {
        UserProfileDto.NickNameUpdateResponse response =
                new UserProfileDto.NickNameUpdateResponse("새 닉네임");

        given(userProfileService.updateNickName(anyLong(), anyString())).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "새 닉네임")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickName").value("새 닉네임"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("nickName").description("수정할 닉네임 (2~20자, 영문·숫자·한글·공백)")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.nickName").description("수정된 닉네임")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("닉네임 수정 성공 - 공백 포함")
    void 닉네임_수정_성공_공백포함() throws Exception {
        UserProfileDto.NickNameUpdateResponse response =
                new UserProfileDto.NickNameUpdateResponse("새 닉네임");

        given(userProfileService.updateNickName(anyLong(), anyString())).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "새 닉네임")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickName").value("새 닉네임"));
    }

    @Test
    @WithCustomUser
    @DisplayName("닉네임 수정 실패 - 빈 값")
    void 닉네임_수정_실패_빈값() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("닉네임 수정 실패 - 특수문자 포함 패턴 불일치")
    void 닉네임_수정_실패_패턴불일치() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "nick!!")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("닉네임 수정 실패 - 유저 없음")
    void 닉네임_수정_실패_유저없음() throws Exception {
        given(userProfileService.updateNickName(anyLong(), anyString()))
                .willThrow(new CustomException(AuthErrorCode.USER_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "닉네임")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없음 - 닉네임 수정 401")
    void 인증없음_닉네임수정_401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickName", "닉네임"))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/users/me/profile-image
    // ─────────────────────────────────────────────────────────

    @Test
    @WithCustomUser
    @DisplayName("프로필 이미지 수정 성공")
    void 프로필이미지_수정_성공() throws Exception {
        UserProfileDto.ProfileImageUpdateResponse response =
                new UserProfileDto.ProfileImageUpdateResponse("https://presigned-url.example.com/profile.png");

        given(userProfileService.updateProfileImage(anyLong(), anyString())).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("profileImageKey", "profile/users/1/uuid.png")
                        ))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profileImageUrl").exists())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("profileImageKey").description("업로드된 프로필 이미지 key")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.profileImageUrl").description("수정된 프로필 이미지 CDN URL")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("프로필 이미지 수정 실패 - 빈 key")
    void 프로필이미지_수정_실패_빈키() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("profileImageKey", "")))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("프로필 이미지 수정 실패 - 유저 없음")
    void 프로필이미지_수정_실패_유저없음() throws Exception {
        given(userProfileService.updateProfileImage(anyLong(), anyString()))
                .willThrow(new CustomException(AuthErrorCode.USER_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("profileImageKey", "profile/users/1/uuid.png")
                        ))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없음 - 프로필 이미지 수정 401")
    void 인증없음_프로필이미지수정_401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("profileImageKey", "profile/users/1/uuid.png")
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithCustomUser
    @DisplayName("프로필 정보 수정 성공")
    void 프로필정보_수정_성공() throws Exception {
        UserProfileDto.ProfileUpdateResponse response =
                new UserProfileDto.ProfileUpdateResponse(
                        "새닉네임",
                        "https://presigned-url.example.com/profile.png"
                );
        given(userProfileService.updateProfile(anyLong(), anyString(), anyString())).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickName", "새닉네임",
                                "profileImageKey", "profile/users/1/uuid.png"
                        )))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS-200"))
                .andExpect(jsonPath("$.result.nickName").value("새닉네임"))
                .andExpect(jsonPath("$.result.profileImageUrl").value("https://presigned-url.example.com/profile.png"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("nickName").description("수정할 닉네임 (2~20자, 영문·숫자·한글)"),
                                fieldWithPath("profileImageKey").description("업로드된 프로필 이미지 key")
                        ),
                        responseFields(ApiResponseSnippet.withResult(
                                fieldWithPath("result.nickName").description("수정된 닉네임"),
                                fieldWithPath("result.profileImageUrl").description("수정된 프로필 이미지 CDN URL")
                        ))
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("프로필 정보 수정 실패 - 닉네임 누락")
    void 프로필정보_수정_실패_닉네임누락() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "profileImageKey", "profile/users/1/uuid.png"
                        )))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }
}
