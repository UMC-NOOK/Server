package app.nook.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDTO {

    @Getter
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResponse {
        private Long id;
        private String email;
        private String nickName;
        private String accessToken;
        private String refreshToken;
        private boolean onboardingCompleted;

        // 탈퇴 유예중 계정: recoveryRequired=true, recoveryToken(1시간) 발급, access/refresh 는 null
        // 일반 로그인 응답에는 두 필드 모두 null → NON_NULL 로 직렬화에서 제외됨
        private Boolean recoveryRequired;
        private String recoveryToken;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class DevLoginRequest{
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바르지 않은 이메일 형식입니다.")
        private String email;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,20}$", message = "닉네임은 2~20자의 영문, 숫자, 한글만 사용할 수 있습니다.")
        private String nickName;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class DevSignUpRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바르지 않은 이메일 형식입니다.")
        private String email;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,20}$", message = "닉네임은 2~20자의 영문, 숫자, 한글만 사용할 수 있습니다.")
        private String nickName;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class TokenReissueRequest {
        @NotBlank(message = "리프레시토큰은 필수입니다.")
        private String refreshToken;
    }

    public record RecoveryRequest(
            @NotBlank(message = "복구 토큰은 필수입니다.")
            String recoveryToken
    ) {}

    public record TokenReissueResponse(
            String accessToken,
            String refreshToken
    ) {}

    public record UserInfo(
            Long id,
            String email,
            String nickName
    ) {}
}
