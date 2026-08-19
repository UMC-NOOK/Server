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

    public static FieldDescriptor[] failureResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("isSuccess")
                        .type(JsonFieldType.BOOLEAN)
                        .description("요청 성공 여부. 오류 응답에서는 false"),

                fieldWithPath("code")
                        .type(JsonFieldType.STRING)
                        .description("오류 코드"),

                fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("오류 메시지"),

                fieldWithPath("result")
                        .type(JsonFieldType.VARIES)
                        .description("오류 상세. 일반 오류에서는 응답에서 제외되고, 검증 오류에서는 필드별 오류 정보를 반환")
                        .optional()
        };
    }

    public static FieldDescriptor[] validationFailureResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("isSuccess")
                        .type(JsonFieldType.BOOLEAN)
                        .description("요청 성공 여부. 오류 응답에서는 false"),

                fieldWithPath("code")
                        .type(JsonFieldType.STRING)
                        .description("오류 코드"),

                fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("오류 메시지"),

                fieldWithPath("result")
                        .type(JsonFieldType.OBJECT)
                        .description("필드명과 검증 오류 메시지로 구성된 객체"),

                fieldWithPath("result.*")
                        .type(JsonFieldType.STRING)
                        .description("각 필드의 검증 오류 메시지")
        };
    }

    public static FieldDescriptor[] withResult(FieldDescriptor... resultFields) {
        return Stream.concat(
                Arrays.stream(commonResponseFields()),
                Arrays.stream(resultFields)
        ).toArray(FieldDescriptor[]::new);
    }
}
