package app.nook.user.filter;

import app.nook.global.response.AuthErrorCode;
import app.nook.global.exception.CustomException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import app.nook.user.handler.SecurityErrorResponseWriter;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        }catch (JwtException ex) {
            setErrorResponse(request, response, ex);
        } catch (CustomException ex) {
            throw ex;
        }
    }

    public void setErrorResponse(HttpServletRequest req, HttpServletResponse res, Throwable ex)
            throws IOException {

        if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            SecurityErrorResponseWriter.write(res, AuthErrorCode.TOKEN_EXPIRED);
            return;
        }

        SecurityErrorResponseWriter.write(res, AuthErrorCode.INVALID_TOKEN);
    }

}
