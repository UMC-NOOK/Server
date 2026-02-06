package app.nook.controller.library;

import app.nook.global.common.AbstractRestDocsTests;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.service.LibraryService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.service.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryControllerTest extends AbstractRestDocsTests {

    @MockitoBean
    private LibraryService libraryService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 서재_책_등록_성공() throws Exception {
        // given
        User user = User.builder()
                .email("jiwon@kakao.com")
                .nickName("jiwon")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        willDoNothing().given(libraryService).save(any(), anyLong());

        // when & then
        mockMvc.perform(
                        post("/api/library/{bookId}", 1L)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에 추가할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }

    @Test
    void 서재_책_삭제_성공() throws Exception {
        // given
        User user = User.builder()
                .email("jiwon@kakao.com")
                .nickName("jiwon")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        willDoNothing().given(libraryService).deleteById(any(), anyLong());

        // when & then
        mockMvc.perform(
                        delete("/api/library/{bookId}", 1L)
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        pathParameters(
                                parameterWithName("bookId").description("서재에서 삭제할 도서 ID")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }

    @Test
    void 서재_책_상태변경_성공() throws Exception {
        // given
        User user = User.builder()
                .email("jiwon@kakao.com")
                .nickName("jiwon")
                .provider("kakao")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        ReadingStatusRequestDto request = new ReadingStatusRequestDto(1L, ReadingStatus.READING);

        willDoNothing().given(libraryService).changeStatus(any(), any());

        // when & then
        mockMvc.perform(
                        patch("/api/library/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header("Authorization", "Bearer test-access-token")
                                .with(user(userDetails))
                )
                .andExpect(status().isOk())
                .andDo(documentWithAuth(
                        "{class-name}/{method-name}",
                        requestFields(
                                fieldWithPath("bookId").description("상태 변경할 도서 ID"),
                                fieldWithPath("readingStatus").description("독서 상태 (READING, FINISHED, BEFORE)")
                        ),
                        responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                ));
    }
}
