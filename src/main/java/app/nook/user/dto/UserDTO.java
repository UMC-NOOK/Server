package app.nook.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class DevLoginRequest{
        private String email;
        private String nickName;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class DevSignUpRequest {
        private String email;
        private String nickName;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class TokenReissueRequest {
        @NotBlank
        private String refreshToken;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public static class TokenReissueResponse {
        private String accessToken;
    }
}
