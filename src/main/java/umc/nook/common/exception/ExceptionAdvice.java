package umc.nook.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    // javax.validation.ConstraintViolationException (ex: @RequestParam 검증 실패)
    @ExceptionHandler
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("잘못된 요청입니다.");
        ApiResponse<Object> body = ApiResponse.onFailure(ErrorCode.INVALID_PASSWORD, message);
        return handleExceptionInternal(e, body, new HttpHeaders(), ErrorCode.INVALID_PASSWORD.getHttpStatus(), request);
    }

    // ✅ @Valid @RequestBody DTO 검증 실패
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (existing, newMsg) -> existing + ", " + newMsg);
        });

        ApiResponse<Object> body = ApiResponse.onFailure(ErrorCode.INVALID_PASSWORD, errors);
        return handleExceptionInternal(e, body, headers, ErrorCode.INVALID_PASSWORD.getHttpStatus(), request);
    }

    // 모든 예외 catch (서버 에러)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnknownException(Exception e, WebRequest request) {
        e.printStackTrace();
        ApiResponse<Object> body = ApiResponse.onFailure(ErrorCode.USER_NOT_FOUND, e.getMessage());
        return handleExceptionInternal(e, body, new HttpHeaders(), ErrorCode.USER_NOT_FOUND.getHttpStatus(), request);
    }

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException e, HttpServletRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(e.getErrorCode(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return handleExceptionInternal(e, body, new HttpHeaders(), e.getErrorCode().getHttpStatus(), webRequest);
    }
}
