package app.nook.global.exception;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.CommonErrorCode;
import app.nook.redis.exception.RedisOperationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.OptimisticLockException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    // @RequestParam, @PathVariable 등 Bean Validation 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("잘못된 요청입니다.");

        ApiResponse<Object> body = ApiResponse.onFailure(
                CommonErrorCode.INVALID_REQUEST,
                message
        );
        return handleExceptionInternal(e, body, new HttpHeaders(),
                CommonErrorCode.INVALID_REQUEST.getHttpStatus(),
                request);
    }

    // DB unique 제약조건 위반 → 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e, WebRequest request) {
        log.warn("DataIntegrityViolationException: {}", e.getMostSpecificCause().getMessage());
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.DUPLICATE_RESOURCE, null);
        return handleExceptionInternal(e, body, new HttpHeaders(),
                CommonErrorCode.DUPLICATE_RESOURCE.getHttpStatus(), request);
    }

    // 요청 바디 파싱 실패 (malformed JSON 등) — Jackson 내부 메시지 노출 차단
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        log.debug("HttpMessageNotReadableException: {}", e.getMessage());
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.INVALID_FORMAT, null);
        return handleExceptionInternal(e, body, headers,
                CommonErrorCode.INVALID_FORMAT.getHttpStatus(), request);
    }

    // @PathVariable, @RequestParam 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e,
                                                                   WebRequest request) {
        log.debug("MethodArgumentTypeMismatchException: param={}, value={}", e.getName(), e.getValue());
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, null);
        return handleExceptionInternal(e, body, new HttpHeaders(),
                CommonErrorCode.INVALID_REQUEST.getHttpStatus(), request);
    }

    // @RequestParam 누락
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = String.format("필수 파라미터 '%s'가 누락되었습니다.", e.getParameterName());
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, message);
        return handleExceptionInternal(e, body, headers,
                CommonErrorCode.INVALID_REQUEST.getHttpStatus(), request);
    }

    // @Valid @RequestBody DTO 검증 실패
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String field = fieldError.getField();
            String msg = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(field, msg, (a, b) -> a + ", " + b);
        });

        ApiResponse<Object> body = ApiResponse.onFailure(
                CommonErrorCode.INVALID_REQUEST,
                errors
        );
        return handleExceptionInternal(e, body, headers,
                CommonErrorCode.INVALID_REQUEST.getHttpStatus(),
                request);
    }

    // 모든 미처리 예외 → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnknownException(Exception e, WebRequest request) {
        log.error("Unhandled exception", e); // printStackTrace() 지양
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.INTERNAL_SERVER_ERROR, null); // 내부 메시지 노출 X
        return handleExceptionInternal(e, body, new HttpHeaders(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), request);
    }

    // 도메인 CustomException
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException e, HttpServletRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(e.getErrorCode(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return handleExceptionInternal(e, body, new HttpHeaders(), e.getErrorCode().getHttpStatus(), webRequest);
    }

    // Redis 예외
    @ExceptionHandler(RedisOperationException.class)
    public ResponseEntity<Object> handleRedisOperationException(RedisOperationException e, HttpServletRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(e.getErrorCode(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return handleExceptionInternal(e, body, new HttpHeaders(), e.getErrorCode().getHttpStatus(), webRequest);
    }

    // 데이터 포맷 오류
    @ExceptionHandler({InvalidFormatException.class})
    public ResponseEntity<Object> handleInvalidDateFormat(InvalidFormatException ex, WebRequest request) {
        CommonErrorCode errorCode = (ex.getTargetType() == LocalDate.class)
                ? CommonErrorCode.INVALID_DATE
                : CommonErrorCode.INVALID_FORMAT;
        ApiResponse<Object> body = ApiResponse.onFailure(errorCode, null);
        return handleExceptionInternal(ex, body, new HttpHeaders(), errorCode.getHttpStatus(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException e, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String field = fieldError.getField();
            String msg = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(field, msg, (a, b) -> a + ", " + b);
        });

        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, errors);
        return handleExceptionInternal(
                e, body, new HttpHeaders(), CommonErrorCode.INVALID_REQUEST.getHttpStatus(), request
        );
    }

    // 락 예외
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Object> handleOptimisticLockException(Exception e, HttpServletRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(CommonErrorCode.CONCURRENT_MODIFICATION, null);
        WebRequest webRequest = new ServletWebRequest(request);
        return handleExceptionInternal(
                e,
                body,
                new HttpHeaders(),
                CommonErrorCode.CONCURRENT_MODIFICATION.getHttpStatus(),
                webRequest
        );
    }
}
