package app.nook.global.docs;

import app.nook.global.exception.CustomException;
import app.nook.global.exception.ExceptionAdvice;
import app.nook.global.response.AuthErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class ExceptionResponseDocumentationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionResponseFixtureController())
                .setControllerAdvice(new ExceptionAdvice())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void badRequest() throws Exception {
        documentError("bad-request", 400, "ACCOUNT-014");
    }

    @Test
    void unauthorized() throws Exception {
        documentError("unauthorized", 401, "ACCOUNT-007");
    }

    @Test
    void forbidden() throws Exception {
        documentError("forbidden", 403, "ACCOUNT-009");
    }

    @Test
    void notFound() throws Exception {
        documentError("not-found", 404, "ACCOUNT-001");
    }

    @Test
    void conflict() throws Exception {
        documentError("conflict", 409, "ACCOUNT-008");
    }

    @Test
    void validationFailure() throws Exception {
        mockMvc.perform(post("/docs/error/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"))
                .andExpect(jsonPath("$.result.name").value("이름은 필수입니다."))
                .andDo(document(
                        "common-error-response/validation",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(ApiResponseSnippet.validationFailureResponseFields())
                ));
    }

    @Test
    void internalServerError() throws Exception {
        mockMvc.perform(get("/docs/error/internal-server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andDo(document(
                        "common-error-response/internal-server-error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(ApiResponseSnippet.failureResponseFields())
                ));
    }

    private void documentError(String errorType, int statusCode, String responseCode)
            throws Exception {
        mockMvc.perform(get("/docs/error/{errorType}", errorType))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(responseCode))
                .andDo(document(
                        "common-error-response/" + errorType,
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(ApiResponseSnippet.failureResponseFields())
                ));
    }

    @RestController
    static class ExceptionResponseFixtureController {

        @GetMapping("/docs/error/{errorType}")
        void error(@PathVariable String errorType) {
            throw switch (errorType) {
                case "bad-request" -> new CustomException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
                case "unauthorized" -> new CustomException(AuthErrorCode.UNAUTHORIZED);
                case "forbidden" -> new CustomException(AuthErrorCode.PERMISSION_DENIED);
                case "not-found" -> new CustomException(AuthErrorCode.USER_NOT_FOUND);
                case "conflict" -> new CustomException(AuthErrorCode.EMAIL_DUPLICATE);
                case "internal-server-error" -> new IllegalStateException("문서화용 서버 오류");
                default -> new IllegalArgumentException("지원하지 않는 오류 유형입니다.");
            };
        }

        @org.springframework.web.bind.annotation.PostMapping("/docs/error/validation")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }
    }

    record ValidationRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }
}
