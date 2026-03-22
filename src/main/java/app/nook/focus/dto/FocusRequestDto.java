package app.nook.focus.dto;

public class FocusRequestDto {

    public record FocusStart (
            Long libraryId,
            Long themeId
    ) {}

}
