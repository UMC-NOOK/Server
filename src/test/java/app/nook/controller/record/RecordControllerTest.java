package app.nook.controller.record;

import app.nook.global.common.AbstractWebMvcRestDocsTests;
import app.nook.global.common.security.WithCustomUser;
import app.nook.global.config.WebSecurityConfig;
import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.record.controller.RecordController;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.exception.RecordErrorCode;
import app.nook.record.service.RecordService;
import app.nook.record.service.RecordViewService;
import app.nook.record.util.RecordListCursorCodec;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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

    @MockitoBean
    private RecordViewService recordViewService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("독서 기록 목록 조회")
    @Nested
    class GetUserRecords {

        @Test
        @WithCustomUser
        void 독서_기록_목록_조회_성공() throws Exception {
            // given
            BookRecordDto.BookRecordItemDto firstItem = new BookRecordDto.BookRecordItemDto(
                    101L,
                    "작별하지 않는다",
                    "한강",
                    "가장 오래 남았던 문장을 기록한 독서 메모입니다.",
                    "https://cdn.example.com/books/101.png",
                    4L
            );
            BookRecordDto.BookRecordItemDto secondItem = new BookRecordDto.BookRecordItemDto(
                    99L,
                    "소년이 온다",
                    "한강",
                    "감정이 크게 남은 부분을 짧게 정리한 기록입니다.",
                    "https://cdn.example.com/books/99.png",
                    2L
            );
            String encodedCursor = RecordListCursorCodec.encode(
                    new RecordListCursor(null, 99L, java.time.LocalDateTime.of(2026, 4, 1, 9, 30))
            );
            CursorResponse<BookRecordDto.BookRecordItemDto, String> response = CursorResponse.of(
                    List.of(firstItem, secondItem),
                    encodedCursor,
                    true
            );

            given(recordViewService.getUserRecords(any(), anyInt(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records")
                            .header(AUTH_HEADER, AUTH_TOKEN)
                            .param("size", "2")
                            .param("order", SortType.RECENT_RECORDED.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.items[0].bookId").value(101))
                    .andExpect(jsonPath("$.result.items[0].title").value("작별하지 않는다"))
                    .andExpect(jsonPath("$.result.nextCursor").value(encodedCursor))
                    .andExpect(jsonPath("$.result.hasNext").value(true))
                    .andDo(documentWithAuth(
                            "record-controller-test/독서_기록_목록_조회_성공",
                            queryParameters(
                                    parameterWithName("cursor").description("다음 페이지 조회에 사용할 단일 커서 문자열. 이전 응답의 result.nextCursor 값을 그대로 전달").optional(),
                                    parameterWithName("size").description("한 번에 조회할 독서 기록 묶음 수. 기본값은 20").optional(),
                                    parameterWithName("order").description("정렬 기준. RECENT_RECORDED, OLDEST_RECORDED, RECORD_COUNT_ASC, RECORD_COUNT_DESC 중 하나 사용").optional()
                            ),
                            responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result.items").type(JsonFieldType.ARRAY).description("사용자의 독서 기록을 책 단위로 묶어 반환한 목록"),
                                    fieldWithPath("result.items[].bookId").type(JsonFieldType.NUMBER).description("독서 기록이 연결된 도서 ID"),
                                    fieldWithPath("result.items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                                    fieldWithPath("result.items[].author").type(JsonFieldType.STRING).description("도서 저자명"),
                                    fieldWithPath("result.items[].recordContent").type(JsonFieldType.STRING).description("해당 도서에서 가장 최근에 작성된 기록 내용. 목록 카드에서 대표 미리보기로 사용"),
                                    fieldWithPath("result.items[].coverImageUrl").type(JsonFieldType.STRING).description("도서 표지 이미지 URL. 책 목록 화면에서 대표 이미지로 사용"),
                                    fieldWithPath("result.items[].recordCount").type(JsonFieldType.NUMBER).description("사용자가 해당 도서에 남긴 전체 기록 수"),
                                    fieldWithPath("result.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 조회에 사용할 인코딩된 단일 커서 문자열").optional(),
                                    fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                            ))
                    ));
        }

        @Test
        @WithCustomUser
        void 독서_기록_목록_조회_데이터없음_204() throws Exception {
            // given
            CursorResponse<BookRecordDto.BookRecordItemDto, String> response = CursorResponse.of(
                    List.of(),
                    null,
                    false
            );

            given(recordViewService.getUserRecords(any(), anyInt(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records")
                            .header(AUTH_HEADER, AUTH_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("SUCCESS-204"))
                    .andDo(documentWithAuth(
                            "record-controller-test/독서_기록_목록_조회_데이터없음_204",
                            queryParameters(
                                    parameterWithName("cursor").description("다음 페이지 조회에 사용할 단일 커서 문자열. 첫 요청이면 전달하지 않음").optional(),
                                    parameterWithName("size").description("한 번에 조회할 독서 기록 묶음 수. 기본값은 20").optional(),
                                    parameterWithName("order").description("정렬 기준. RECENT_RECORDED(최근 독서 기록 순), OLDEST_RECORDED(오래된 기록 순), RECORD_COUNT_ASC(기록 개수 적은 순), RECORD_COUNT_DESC(기록 개수 많은 순) 중 하나 사용").optional()
                            ),
                            responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                    ));
        }
    }

    @DisplayName("특정 책 기록 감정 필터 조회")
    @Nested
    class GetBookRecords {

        @Test
        @WithCustomUser
        void 특정_책_기록_감정_필터_조회_성공() throws Exception {
            // given
            BookRecordDto.RecordItemDto firstItem = new BookRecordDto.RecordItemDto(
                    31L,
                    "가장 인상 깊었던 장면을 적어둔 기록",
                    List.of(
                            "https://example.com/records/31-1.png",
                            "https://example.com/records/31-2.png"
                    ),
                    Emotion.FUN,
                    java.time.LocalDate.of(2026, 4, 5)
            );
            BookRecordDto.RecordItemDto secondItem = new BookRecordDto.RecordItemDto(
                    27L,
                    "같은 책에서 다시 웃겼던 포인트를 정리한 기록",
                    List.of("https://example.com/records/27-1.png"),
                    Emotion.FUN,
                    java.time.LocalDate.of(2026, 4, 1)
            );
            CursorResponse<BookRecordDto.RecordItemDto, Long> response = CursorResponse.of(
                    List.of(firstItem, secondItem),
                    27L,
                    true
            );

            given(recordViewService.getBookRecords(any(), anyLong(), anyInt(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records/books/{bookId}", 101L)
                            .header(AUTH_HEADER, AUTH_TOKEN)
                            .param("size", "2")
                            .param("emotion", "FUN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.items[0].recordId").value(31))
                    .andExpect(jsonPath("$.result.items[0].imgUrls[0]").value("https://example.com/records/31-1.png"))
                    .andExpect(jsonPath("$.result.items[0].emotion").value("FUN"))
                    .andExpect(jsonPath("$.result.nextCursor").value(27))
                    .andDo(documentWithAuth(
                            "record-controller-test/특정_책_기록_감정_필터_조회_성공",
                            pathParameters(
                                    parameterWithName("bookId").description("기록을 조회할 도서 ID")
                            ),
                            queryParameters(
                                    parameterWithName("cursor").description("다음 페이지 조회에 사용할 커서 값. 첫 요청이면 전달하지 않음").optional(),
                                    parameterWithName("size").description("한 번에 조회할 기록 수. 기본값은 20").optional(),
                                    parameterWithName("emotion").description("감정 필터. ALL, FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE 중 하나 사용. ALL이면 전체 반환").optional()
                            ),
                            responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result.items").type(JsonFieldType.ARRAY).description("특정 책에 남긴 기록 목록. 최신 기록 순으로 반환"),
                                    fieldWithPath("result.items[].recordId").type(JsonFieldType.NUMBER).description("기록 ID"),
                                    fieldWithPath("result.items[].content").type(JsonFieldType.STRING).description("기록 내용"),
                                    fieldWithPath("result.items[].imgUrls").type(JsonFieldType.ARRAY).description("기록에 연결된 이미지 조회 URL 목록. 저장된 key를 기준으로 조회 시점에 presigned GET URL로 변환"),
                                    fieldWithPath("result.items[].emotion").type(JsonFieldType.STRING).description("기록 감정 값"),
                                    fieldWithPath("result.items[].createdDate").type(JsonFieldType.STRING).description("기록 생성 날짜"),
                                    fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).description("다음 페이지 조회에 사용할 커서 값. 다음 페이지가 없으면 null").optional(),
                                    fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                            ))
                    ));
        }

        @Test
        @WithCustomUser
        void 특정_책_기록_감정_필터_조회_데이터없음_204() throws Exception {
            // given
            CursorResponse<BookRecordDto.RecordItemDto, Long> response = CursorResponse.of(
                    List.of(),
                    null,
                    false
            );

            given(recordViewService.getBookRecords(any(), anyLong(), anyInt(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records/books/{bookId}", 101L)
                            .header(AUTH_HEADER, AUTH_TOKEN)
                            .param("emotion", "ALL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS-204"))
                    .andDo(documentWithAuth(
                            "record-controller-test/특정_책_기록_감정_필터_조회_데이터없음_204",
                            pathParameters(
                                    parameterWithName("bookId").description("기록을 조회할 도서 ID")
                            ),
                            queryParameters(
                                    parameterWithName("cursor").description("다음 페이지 조회에 사용할 커서 값. 첫 요청이면 전달하지 않음").optional(),
                                    parameterWithName("size").description("한 번에 조회할 기록 수. 기본값은 20").optional(),
                                    parameterWithName("emotion").description("감정 필터. ALL, FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE 중 하나 사용. ALL이면 전체 반환").optional()
                            ),
                            responseFields(ApiResponseSnippet.commonResponseFieldsWithNullableResult())
                    ));
        }
    }

    @DisplayName("독서 기록 감정별 개수 조회")
    @Nested
    class GetRecordEmotionCounts {

        @Test
        @WithCustomUser
        void 독서_기록_감정별_개수_조회_성공() throws Exception {
            // given
            BookRecordDto.RecordEmotionCountResponse response = new BookRecordDto.RecordEmotionCountResponse(
                    7L,
                    List.of(
                            new BookRecordDto.RecordEmotionDto(Emotion.FUN, 3L),
                            new BookRecordDto.RecordEmotionDto(Emotion.EMPATHIZING, 2L),
                            new BookRecordDto.RecordEmotionDto(Emotion.USEFUL, 2L)
                    )
            );

            given(recordViewService.getRecordEmotionCounts(any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/records/emotions")
                            .header(AUTH_HEADER, AUTH_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalCount").value(7))
                    .andExpect(jsonPath("$.result.emotionCounts[0].emotion").value("FUN"))
                    .andExpect(jsonPath("$.result.emotionCounts[0].recordCount").value(3))
                    .andDo(documentWithAuth(
                            "record-controller-test/독서_기록_감정별_개수_조회_성공",
                            responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result.totalCount").type(JsonFieldType.NUMBER).description("사용자가 작성한 전체 독서 기록 수"),
                                    fieldWithPath("result.emotionCounts").type(JsonFieldType.ARRAY).description("감정별로 집계한 독서 기록 개수 목록"),
                                    fieldWithPath("result.emotionCounts[].emotion").type(JsonFieldType.STRING).description("기록에 저장된 감정 값. FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE 중 하나"),
                                    fieldWithPath("result.emotionCounts[].recordCount").type(JsonFieldType.NUMBER).description("해당 감정으로 작성된 독서 기록 수")
                            ))
                    ));
        }
    }

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
                mockMvc.perform(post("/api/v1/records/books/{bookId}", 1L)
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
                                        fieldWithPath("emotion").type(JsonFieldType.STRING).description("기록 감정 값. FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE 중 하나"),
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
                mockMvc.perform(post("/api/v1/records/books/{bookId}", 1L)
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
            mockMvc.perform(put("/api/v1/records/{recordId}", 10L)
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
                                    fieldWithPath("emotion").type(JsonFieldType.STRING).description("수정할 기록 감정 값. FUN, EMPATHIZING, USEFUL, COMPLICATED, SAD, UNCOMFORTABLE 중 하나"),
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
                mockMvc.perform(delete("/api/v1/records/{recordId}", 10L)
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
                mockMvc.perform(delete("/api/v1/records/{recordId}", 999L)
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
            mockMvc.perform(get("/api/v1/records/count")
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
