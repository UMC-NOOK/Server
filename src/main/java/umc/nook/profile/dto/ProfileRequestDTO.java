package umc.nook.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "사용자 별명", example = "프로 독자")
    private String alias;

    @Schema(
            description = "캐릭터 색상",
            example = "ORANGE",
            allowableValues = {"BLUE", "RED", "ORANGE", "GREEN"}
    )
    private CharacterColor characterColor;

    @Schema(
            description = "배경 패턴",
            example = "NONE",
            allowableValues = {"NONE", "STRIPE", "ARGYLE", "DOT", "PLAID", "STAR"}
    )
    private BackgroundPattern backgroundPattern;
}
