package umc.nook.users.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import umc.nook.users.domain.User;

public class UserDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUpDto {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 유효하지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 7, max = 20, message = "비밀번호는 7자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{7,20}$",
                message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
        private String password;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
        private String nickname;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginDto {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 유효하지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequestDto {
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        private String refreshToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenResponseDto {
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    public static class LoginResponseDTO {

        private Long userId;
        private String email;
        private String nickname;
        private TokenResponseDto token;

        public LoginResponseDTO(User user, TokenResponseDto token) {
            this.userId = user.getUserId();
            this.email = user.getEmail();
            this.nickname = user.getNickname();
            this.token = token;
        }

    }

    @Getter
    public static class UserResponseDTO {
        private Long userId;
        private String email;
        private String nickname;

        public UserResponseDTO(User user) {
            this.userId = user.getUserId();
            this.email = user.getEmail();
            this.nickname = user.getNickname();
        }
    }

}


