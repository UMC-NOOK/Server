package app.nook.admin;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 여부 판별. 관리자 계정 이메일은 환경변수(ADMIN_EMAILS, 콤마 구분)로 주입된다.
 * 목록이 비어 있으면 관리자 없음 → 관리자 API 는 항상 거부된다.
 */
@Component
public class AdminAccessChecker {

    private final Set<String> adminEmails;

    public AdminAccessChecker(@Value("${admin.emails:}") String emails) {
        this.adminEmails = Arrays.stream(emails.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String email) {
        return email != null && adminEmails.contains(email.toLowerCase());
    }

    /** 관리자가 아니면 403 (PERMISSION_DENIED) */
    public void verifyAdmin(User user) {
        if (user == null || !isAdmin(user.getEmail())) {
            throw new CustomException(AuthErrorCode.PERMISSION_DENIED);
        }
    }
}
