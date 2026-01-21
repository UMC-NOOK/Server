package app.nook.global.common;

import jdk.jshell.Snippet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

@Configuration
public class RestDocsConfiguration {

    @Bean
    public RestDocumentationResultHandler restDocs() {
        return document(
                "{class-name}/{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );
    }

    @Bean
    public RequestHeadersSnippet authorizationHeaderSnippet() {
        return requestHeaders(
                headerWithName("Authorization")
                        .description("Bearer 토큰 (예: Bearer eyJhbGciOiJIUzI1Ni...)")
        );
    }
}
