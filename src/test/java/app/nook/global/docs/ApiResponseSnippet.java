package app.nook.global.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public class ApiResponseSnippet {

    public static FieldDescriptor[] commonResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("isSuccess")
                        .description("요청 성공 여부"),

                fieldWithPath("code")
                        .description("응답 코드"),

                fieldWithPath("message")
                        .description("응답 메시지"),

                fieldWithPath("result")
                        .description("응답 데이터 (API별 상이)")
        };
    }

    public static FieldDescriptor[] withResult(FieldDescriptor resultField) {
        return Stream.concat(
                Arrays.stream(commonResponseFields()),
                Stream.of(resultField)
        ).toArray(FieldDescriptor[]::new);
    }
}
