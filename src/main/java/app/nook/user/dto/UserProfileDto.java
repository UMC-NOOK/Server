package app.nook.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class UserProfileDto {

    public record MyPageResponse(
            Long userId,
            String nickName,
            String email,
            String profileImageUrl
    ) {}


    public record NickNameUpdateRequest(
            @NotBlank
            @Pattern(
                regexp = "^[a-zA-Z0-9가-힣](?:[a-zA-Z0-9가-힣 ]{0,18}[a-zA-Z0-9가-힣])$",
                message = "닉네임은 2~20자의 영문, 숫자, 한글, 공백만 사용할 수 있습니다."
            )
            String nickName
    ) {}

    public record NickNameUpdateResponse(
            String nickName
    ) {}

    public record ProfileImageUpdateRequest(
            @NotBlank
            @Size(max = 512)
            String profileImageKey
    ) {}

    public record ProfileImageUpdateResponse(
            String profileImageUrl
    ) {}

    public record ProfileUpdateRequest(
            @NotBlank
            @Pattern(
                regexp = "^[a-zA-Z0-9가-힣](?:[a-zA-Z0-9가-힣 ]{0,18}[a-zA-Z0-9가-힣])$",
                message = "닉네임은 2~20자의 영문, 숫자, 한글, 공백만 사용할 수 있습니다."
            )
            String nickName,
            @NotBlank
            @Size(max = 512)
            String profileImageKey
    ) {}

    public record ProfileUpdateResponse(
            String nickName,
            String profileImageUrl
    ) {}

}
