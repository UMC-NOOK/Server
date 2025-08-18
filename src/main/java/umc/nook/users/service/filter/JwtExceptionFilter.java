package umc.nook.users.service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.nook.common.exception.CustomException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response); // JwtAuthenticationFilter로 이동
        }catch (JwtException | CustomException ex) {
            setErrorResponse(request, response, ex);
        }
    }

    public void setErrorResponse(HttpServletRequest req, HttpServletResponse res, Throwable ex)
            throws IOException {

        if (res.isCommitted()) return;

        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");

        String code;
        String detail;

        // 예외 타입별 분기
        if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            code = "TOKEN_EXPIRED";
            detail = "토큰이 만료되었습니다.";
        } else if (ex instanceof io.jsonwebtoken.SignatureException) {
            code = "TOKEN_SIGNATURE_INVALID";
            detail = "토큰 서명이 유효하지 않습니다.";
        } else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
            code = "TOKEN_MALFORMED";
            detail = "토큰 형식이 올바르지 않습니다.";
        } else if (ex instanceof io.jsonwebtoken.UnsupportedJwtException) {
            code = "TOKEN_UNSUPPORTED";
            detail = "지원하지 않는 토큰입니다.";
        } else if (ex instanceof IllegalArgumentException) {
            code = "TOKEN_MISSING";
            detail = "토큰이 비어있거나 잘못되었습니다.";
        } else {
            code = "TOKEN_INVALID";
            detail = "토큰이 유효하지 않습니다.";
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", java.time.OffsetDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "UNAUTHORIZED");
        body.put("code", code);
        body.put("detail", detail);
        body.put("path", req.getRequestURI());

        new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValue(res.getOutputStream(), body);
    }

}

