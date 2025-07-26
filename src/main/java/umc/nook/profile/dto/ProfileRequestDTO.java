package umc.nook.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.nook.profile.domain.BackgroundPattern;
import umc.nook.profile.domain.CharacterColor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequestDTO {
    private String alias;
    private CharacterColor characterColor;
    private BackgroundPattern backgroundPattern;
}
