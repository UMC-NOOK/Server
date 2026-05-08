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

    public record TokenReissueResponse(
            String accessToken,
            String refreshToken
    ) {}
}
