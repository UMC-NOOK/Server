package umc.nook.users.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecurityExceptionDTO {

    private int statusCode;
    private String msg;

    public SecurityExceptionDTO(int statusCode, String msg) {
        this.statusCode = statusCode;
        this.msg = msg;
    }
}

