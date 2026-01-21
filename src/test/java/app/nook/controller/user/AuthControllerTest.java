package app.nook.controller.user;

import app.nook.global.common.AbstractRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.oauth.OAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractRestDocsTests {

    @MockBean
    private OAuthService oAuthService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 소셜로그인_성공() throws Exception {
        // given
        OAuthDTO.OAuthLoginRequest request =
                new OAuthDTO.OAuthLoginRequest("auth-code");

        UserDTO.LoginResponse response =
                UserDTO.LoginResponse.builder()
                        .email()
                        .build();

        given(oAuthService.login(any(), any(), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/auth/oauth/{provider}", "google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("provider")
                                        .description("OAuth 제공자 (google, kakao)")
                        ),
                        requestFields(
                                fieldWithPath("code")
                                        .description("OAuth 인가 코드")
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
}