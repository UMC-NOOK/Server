package app.nook.user.handler;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.BaseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

import java.io.IOException;

// 인증 오류 응답을 작성하는 유틸리티 클래스
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void write(HttpServletResponse response, BaseCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> body = ApiResponse.onFailure(errorCode, null);
        OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
    }
}
