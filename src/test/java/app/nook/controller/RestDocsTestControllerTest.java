package app.nook.controller;

import app.nook.global.docs.ApiResponseSnippet;
import app.nook.global.test.RestDocsTestController;
import app.nook.global.common.AbstractRestDocsTests;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RestDocs 테스트 및 설명을 위한 test case입니다.
 */

class RestDocsTestControllerTest extends AbstractRestDocsTests {

    @Test
    void RestDocsTest() throws Exception {
        mockMvc.perform(get("/restDocsTest"))
                .andExpect(status().isOk())
                .andDo(restDocs.document());
    }

    @Test
    void apiResponseTest_docs() throws Exception {
        mockMvc.perform(get("/apiResponseTest"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(ApiResponseSnippet.withResult(
                                    fieldWithPath("result").description("테스트 문자열 결과")
                                )
                        )
                    )
                );
    }
}