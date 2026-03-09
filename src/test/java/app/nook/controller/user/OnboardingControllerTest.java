package app.nook.controller.user;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.controller.OnboardingController;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.service.OnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OnboardingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
public class OnboardingControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private OnboardingService onboardingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithCustomUser
    @DisplayName("온보딩 완료 성공")
    void 온보딩완료_성공() throws Exception {
        LocalDateTime completedAt = LocalDateTime.of(2026, 3, 8, 12, 0, 0);
        OnboardingDto.CompleteResponse response =
                new OnboardingDto.CompleteResponse(true, "에세이", completedAt);

        given(onboardingService.completeOnboarding(anyLong(), any())).willReturn(response);

        MockMultipartFile profile = new MockMultipartFile(
                "profileImage", "profile.png", "image/png", "img".getBytes()
        );
        MockPart goal = new MockPart("goal", "120".getBytes());
        MockPart nickname = new MockPart("nickname", "nick_01".getBytes());
        MockPart categories = new MockPart("categories", "에세이".getBytes());

        mockMvc.perform(multipart("/api/users/me/onboarding/complete")
                        .file(profile)
                        .part(goal)
                        .part(nickname)
                        .part(categories)
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.result.preferredCategory").value("에세이"))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestParts(
                                partWithName("goal").description("독서 목표 권수 (1~300)"),
                                partWithName("nickname").description("닉네임 (최대 10자)"),
                                partWithName("categories").description("선호 카테고리 (1~2개)"),
                                partWithName("profileImage").description("프로필 이미지 파일").optional()
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.onboardingCompleted").description("온보딩 완료 여부"),
                                        fieldWithPath("result.preferredCategory").description("저장된 선호 카테고리"),
                                        fieldWithPath("result.completedAt").description("온보딩 완료 시각")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("온보딩 완료 실패 - goal 범위 초과")
    void 온보딩완료_실패_goal범위초과() throws Exception {
        mockMvc.perform(multipart("/api/users/me/onboarding/complete")
                        .param("goal", "301")
                        .param("nickname", "nick_01")
                        .param("categories", "에세이")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("온보딩 완료 실패 - categories 누락")
    void 온보딩완료_실패_categories누락() throws Exception {
        mockMvc.perform(multipart("/api/users/me/onboarding/complete")
                        .param("goal", "100")
                        .param("nickname", "nick_01")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithCustomUser
    @DisplayName("온보딩 상태 조회 성공")
    void 온보딩상태_조회_성공() throws Exception {
        LocalDateTime completedAt = LocalDateTime.of(2026, 3, 8, 13, 0, 0);
        OnboardingDto.StatusResponse response = new OnboardingDto.StatusResponse(false, completedAt);
        given(onboardingService.getOnboardingStatus(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/users/me/onboarding/status")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.needsOnboarding").value(false))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.needsOnboarding").description("온보딩 필요 여부"),
                                        fieldWithPath("result.completedAt").description("온보딩 완료 시각")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("독서 목표 조회 성공")
    void 독서목표_조회_성공() throws Exception {
        OnboardingDto.GoalResponse response = new OnboardingDto.GoalResponse((short) 100, 73);
        given(onboardingService.getGoal(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/users/me/onboarding/goal")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.goal").value(100))
                .andExpect(jsonPath("$.result.remainingCount").value(73))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.goal").description("독서 목표 권수"),
                                        fieldWithPath("result.remainingCount").description("남은 권수")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("독서 목표 수정 성공")
    void 독서목표_수정_성공() throws Exception {
        OnboardingDto.GoalUpdateResponse response = new OnboardingDto.GoalUpdateResponse((short) 120);
        given(onboardingService.updateGoal(anyLong(), any())).willReturn(response);

        mockMvc.perform(patch("/api/users/me/onboarding/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("goal", 120)))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.goal").value(120))
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("goal").description("수정할 목표 권수 (1~300)")
                        ),
                        responseFields(
                                ApiResponseSnippet.withResult(
                                        fieldWithPath("result.goal").description("수정된 목표 권수")
                                )
                        )
                ));
    }

    @Test
    @WithCustomUser
    @DisplayName("독서 목표 수정 실패 - goal 0")
    void 독서목표_수정_실패_goal0() throws Exception {
        mockMvc.perform(patch("/api/users/me/onboarding/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("goal", 0)))
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없음 - 온보딩 완료 401")
    void 인증없음_complete_401() throws Exception {
        mockMvc.perform(multipart("/api/users/me/onboarding/complete")
                        .param("goal", "100")
                        .param("nickname", "nick_01")
                        .param("categories", "에세이"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없음 - 상태 조회 401")
    void 인증없음_status_401() throws Exception {
        mockMvc.perform(get("/api/users/me/onboarding/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없음 - 목표 조회 401")
    void 인증없음_goal_get_401() throws Exception {
        mockMvc.perform(get("/api/users/me/onboarding/goal"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없음 - 목표 수정 401")
    void 인증없음_goal_patch_401() throws Exception {
        mockMvc.perform(patch("/api/users/me/onboarding/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("goal", 100))))
                .andExpect(status().isUnauthorized());
    }
}
