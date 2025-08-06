package umc.nook.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import umc.nook.profile.domain.BackgroundPattern;
import umc.nook.profile.domain.CharacterColor;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponseDTO {
    private String alias;
    private CharacterColor characterColor;
    private BackgroundPattern backgroundPattern;
}
