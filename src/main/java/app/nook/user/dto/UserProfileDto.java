package app.nook.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
            @NotNull(message = NickName.EMPTY_MESSAGE)
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
            @NotNull(message = NickName.EMPTY_MESSAGE)
            @NickName
            String nickName,
            @Size(max = 512)
            String profileImageKey
    ) {}

    public record ProfileUpdateResponse(
            String nickName,
            String profileImageUrl
    ) {}

}
