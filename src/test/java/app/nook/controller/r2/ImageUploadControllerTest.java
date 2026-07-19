package app.nook.controller.r2;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.r2.controller.ImageUploadController;
import app.nook.r2.dto.ImageUploadRequestDto;
import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.r2.dto.MultipleImageUploadRequestDto;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ImageUploadController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class ImageUploadControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private PresignedUrlService presignedUrlService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("이미지 업로드 URL 발급")
    @Nested
    class IssueUploadUrl {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @WithCustomUser
            void 단건_업로드_URL_발급_성공() throws Exception {
                // given
                ImageUrlResponseDto response = new ImageUrlResponseDto(
                        "https://example.com/upload/book/test.png",
                        "book/users/1/test.png"
                );

                given(presignedUrlService.generateUploadUrl(anyLong(), any()))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/images/upload-url")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ImageUploadRequestDto("book", "image/jpeg")
                                )))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("SUCCESS-200"))
                        .andExpect(jsonPath("$.result.imageUrl").value("https://example.com/upload/book/test.png"))
                        .andExpect(jsonPath("$.result.key").value("book/users/1/test.png"))
                        .andDo(documentWithAuth(
                                "image-upload-controller-test/단건_업로드_URL_발급_성공",
                                requestFields(
                                        fieldWithPath("uploadType").type(JsonFieldType.STRING).description("이미지 업로드 위치 타입(record, book, profile)"),
                                        fieldWithPath("contentType").type(JsonFieldType.STRING).description("이미지 MIME 타입(image/jpeg, image/png, image/webp)")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("Presigned 업로드 URL"),
                                        fieldWithPath("result.key").type(JsonFieldType.STRING).description("저장된 이미지 key")
                                ))
                        ));
            }

            @Test
            @WithCustomUser
            void 다건_업로드_URL_발급_성공() throws Exception {
                // given
                List<ImageUrlResponseDto> response = List.of(
                        new ImageUrlResponseDto(
                                "https://example.com/upload/record/1.png",
                                "record/users/1/first.png"
                        ),
                        new ImageUrlResponseDto(
                                "https://example.com/upload/record/2.png",
                                "record/users/1/second.png"
                        )
                );

                given(presignedUrlService.generateMultipleUploadUrls(anyLong(), any()))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/images/upload-urls")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new MultipleImageUploadRequestDto(List.of(
                                                new ImageUploadRequestDto("record", "image/png"),
                                                new ImageUploadRequestDto("record", "image/webp")
                                        ))
                                )))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("SUCCESS-200"))
                        .andExpect(jsonPath("$.result[0].imageUrl").value("https://example.com/upload/record/1.png"))
                        .andExpect(jsonPath("$.result[0].key").value("record/users/1/first.png"))
                        .andExpect(jsonPath("$.result[1].imageUrl").value("https://example.com/upload/record/2.png"))
                        .andExpect(jsonPath("$.result[1].key").value("record/users/1/second.png"))
                        .andDo(documentWithAuth(
                                "image-upload-controller-test/다건_업로드_URL_발급_성공",
                                requestFields(
                                        fieldWithPath("files").type(JsonFieldType.ARRAY).description("업로드할 이미지 요청 목록"),
                                        fieldWithPath("files[].uploadType").type(JsonFieldType.STRING).description("이미지 업로드 위치 타입(record, book, profile)"),
                                        fieldWithPath("files[].contentType").type(JsonFieldType.STRING).description("이미지 MIME 타입(image/jpeg, image/png, image/webp)")
                                ),
                                responseFields(ApiResponseSnippet.withResult(
                                        fieldWithPath("result[].imageUrl").type(JsonFieldType.STRING).description("Presigned 업로드 URL"),
                                        fieldWithPath("result[].key").type(JsonFieldType.STRING).description("저장된 이미지 key")
                                ))
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @WithCustomUser
            void 단건_업로드_URL_발급_실패_유효하지않은_파일타입() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/images/upload-url")
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ImageUploadRequestDto("record", "image/gif")
                                )))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("COMMON-002"))
                        .andExpect(jsonPath("$.result.contentType").exists());
            }
        }
    }
}
