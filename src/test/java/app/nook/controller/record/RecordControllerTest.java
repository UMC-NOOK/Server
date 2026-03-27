package app.nook.controller.record;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.exception.CustomException;
import app.nook.record.controller.RecordController;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.service.RecordService;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RecordController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        WebSecurityConfig.class,
                        JwtFilter.class,
                        JwtExceptionFilter.class
                }
        )
)
class RecordControllerTest extends AbstractWebMvcRestDocsTests {

    @MockitoBean
    private RecordService recordService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("기록 생성")
    @Nested
    class CreateRecord {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @WithCustomUser
            void 기록_생성_성공() throws Exception {
                // given
                RecordRequestDto request = new RecordRequestDto(
                        "기록 내용입니다.",
                        Emotion.FUN,
                        List.of("record/users/1/first.png")
                );

                willDoNothing().given(recordService).createRecord(any(), anyLong(), any());

                // when & then
                mockMvc.perform(post("/api/records/books/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("SUCCESS-201"))
                        .andDo(documentWithAuth(
                                "record-controller-test/기록_생성_성공",
                                pathParameters(
                                        parameterWithName("bookId").description("기록을 생성할 도서 ID")
                                ),
                                requestFields(
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("기록 내용"),
                                        fieldWithPath("emotion").type(JsonFieldType.STRING).description("기록 감정(FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE)"),
                                        fieldWithPath("imageKeys").type(JsonFieldType.ARRAY).description("업로드 완료된 이미지 key 목록").optional(),
                                        fieldWithPath("imageKeys[]").description("업로드 완료된 record 이미지 key")
                                ),
                                responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @WithCustomUser
            void 기록_생성_실패_내용_누락() throws Exception {
                // given
                RecordRequestDto request = new RecordRequestDto(
                        "",
                        Emotion.FUN,
                        List.of()
                );

                // when & then
                mockMvc.perform(post("/api/records/books/{bookId}", 1L)
                                .header(AUTH_HEADER, AUTH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @DisplayName("기록 수정")
    @Nested
    class UpdateRecord {

        @Test
        @WithCustomUser
        void 기록_수정_성공() throws Exception {
            // given
            RecordUpdateRequestDto request = new RecordUpdateRequestDto(
                    "수정된 기록 내용입니다.",
                    Emotion.EMPATHIZING,
                    List.of(
                            "record/users/1/first.png",
                            "record/users/1/second.png"
                    )
            );

            willDoNothing().given(recordService).updateRecord(any(), anyLong(), any());

            // when & then
            mockMvc.perform(put("/api/records/{recordId}", 10L)
                            .header(AUTH_HEADER, AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("SUCCESS-200"))
                    .andDo(documentWithAuth(
                            "record-controller-test/기록_수정_성공",
                            pathParameters(
                                    parameterWithName("recordId").description("수정할 기록 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 기록 내용"),
                                    fieldWithPath("emotion").type(JsonFieldType.STRING).description("수정할 기록 감정(FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE)"),
                                    fieldWithPath("imageKeys").type(JsonFieldType.ARRAY).description("수정 후 최종 이미지 key 목록").optional(),
                                    fieldWithPath("imageKeys[]").description("업로드 완료된 record 이미지 key")
                            ),
                            responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                    ));
        }
    }

    @DisplayName("기록 삭제")
    @Nested
    class DeleteRecord {

        @DisplayName("성공")
        @Nested
        class Success {

            @Test
            @WithCustomUser
            void 기록_삭제_성공() throws Exception {
                // given
                willDoNothing().given(recordService).deleteRecord(any(), anyLong());

                // when & then
                mockMvc.perform(delete("/api/records/{recordId}", 10L)
                                .header(AUTH_HEADER, AUTH_TOKEN))
                        .andExpect(status().isOk())
                        .andDo(documentWithAuth(
                                "record-controller-test/기록_삭제_성공",
                                pathParameters(
                                        parameterWithName("recordId").description("삭제할 기록 ID")
                                ),
                                responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                        ));
            }
        }

        @DisplayName("실패")
        @Nested
        class Failure {

            @Test
            @WithCustomUser
            void 기록_삭제_실패_기록없음() throws Exception {
                // given
                willThrow(new CustomException(RecordErrorCode.RECORD_NOT_FOUND))
                        .given(recordService).deleteRecord(any(), anyLong());

                // when & then
                mockMvc.perform(delete("/api/records/{recordId}", 999L)
                                .header(AUTH_HEADER, AUTH_TOKEN))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("RECORD-404"));
            }
        }
    }

    @DisplayName("기록 전체 개수 조회")
    @Nested
    class CountRecords {

        @Test
        @WithCustomUser
        void 기록_전체개수_조회_성공() throws Exception {
            // given
            RecordResponseDto.RecordCountDto response = new RecordResponseDto.RecordCountDto(12);
            given(recordService.countRecords(anyLong())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records/count")
                            .header(AUTH_HEADER, AUTH_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.count").value(12))
                    .andDo(documentWithAuth(
                            "record-controller-test/기록_전체개수_조회_성공",
                            responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result.count").type(JsonFieldType.NUMBER).description("사용자의 전체 기록 수")
                            ))
                    ));
        }
    }
}
