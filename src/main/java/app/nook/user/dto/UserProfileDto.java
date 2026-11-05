package app.nook.user.dto;

import jakarta.validation.constraints.NotBlank;
import app.nook.user.validation.NickName;
import jakarta.validation.constraints.Size;


public class UserProfileDto {

    public record MyPageResponse(
            Long userId,
            String nickName,
            String email,
            String profileImageUrl
    ) {}


    public record NickNameUpdateRequest(
            @NickName
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
            @NickName
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
