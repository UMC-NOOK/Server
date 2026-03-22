package app.nook.focus.dto;

public class FocusRequestDto {

    public record FocusStart (
            Long libraryId,
            Long themeId
    ) {}

    public record FocusEnd(
            Long focusId,
            Integer page,
            Boolean isFinished
    ) {}
}
