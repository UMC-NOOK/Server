package app.nook.global.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * 통합 테스트용
 */
@Import(RestDocsConfiguration.class)
@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs
@SpringBootTest(properties = {
        "spring.profiles.active=test"
})
@ActiveProfiles("test")
public abstract class AbstractRestDocsTests extends AbstractPostgresContainerTests {

    protected static final String AUTH_HEADER = "Authorization";
    protected static final String AUTH_TOKEN = "Bearer test-access-token";

    @Autowired
    protected RestDocumentationResultHandler restDocs;

    @Autowired
    protected Snippet authorizationHeaderSnippet;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUp(
            final WebApplicationContext context,
            final RestDocumentationContextProvider restDocumentation) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(MockMvcResultHandlers.print())
                .alwaysDo(restDocs)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    /**
     * - Authorization 헤더를 공통으로 문서화
     * - 인증이 필요한 API는 이 메서드만 사용
    */
    protected ResultHandler documentWithAuth(String identifier, Snippet... snippets) {

        // 공통 Authorization 헤더, API별 스니펫 합치기
        Snippet[] mergedSnippets = Stream.concat(
                Stream.of(authorizationHeaderSnippet),
                Arrays.stream(snippets)
        ).toArray(Snippet[]::new);

        return document(identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                mergedSnippets);
    }
}
