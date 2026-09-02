package app.nook.focus.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FocusRequestDto {

    public record FocusStart (
            @NotNull @Positive Long bookId
    ) {}

    public record FocusEnd(
            @NotNull @Positive Long focusId,
            @Positive Integer page,
            @NotNull Boolean isFinished
    ) {}
}
