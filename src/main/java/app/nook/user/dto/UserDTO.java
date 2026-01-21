package app.nook.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDTO {

    @Getter
    @AllArgsConstructor
    public static class LoginResponse {
        private Long id;
        private String email;
        private String nickName;
    }

    @AllArgsConstructor
    @Getter
    public static class DevLoginRequest{
        private String email;
        private String nickName;
    }
}
