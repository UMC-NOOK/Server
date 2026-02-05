package app.nook.global.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
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
                        .optional()
        };
    }

    public static FieldDescriptor[] commonResponseFieldsWithNullableResult() {
        return new FieldDescriptor[] {
                fieldWithPath("isSuccess")
                        .description("요청 성공 여부"),

                fieldWithPath("code")
                        .description("응답 코드"),

                fieldWithPath("message")
                        .description("응답 메시지"),

                fieldWithPath("result")
                        .description("응답 데이터 (없을 경우 null 또는 미포함)")
                        .type(JsonFieldType.NULL)
                        .optional()
        };
    }

    public static FieldDescriptor[] withResult(FieldDescriptor... resultFields) {
        return Stream.concat(
                Arrays.stream(commonResponseFields()),
                Arrays.stream(resultFields)
        ).toArray(FieldDescriptor[]::new);
    }
}
