package app.nook.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

public class OnboardingDto {

    @Getter
    @Setter
    public static class CompleteRequest {
        @Min(1) @Max(300)
        private short goal;

        @NotBlank @Size(max = 10)
        private String nickname;

        @NotEmpty @Size(min = 1, max = 2)
        private List<String> categories;

        private String profileImageKey;
    }

    public record CompleteResponse(
            boolean onboardingCompleted,
            String preferredCategory,
            LocalDateTime completedAt
    ) {}

    public record StatusResponse(
            boolean needsOnboarding,
            LocalDateTime completedAt
    ) {}

    public record GoalUpdateRequest(
            @Min(1) @Max(300) short goal
    ) {}

    public record GoalUpdateResponse(
            short goal
    ) {}

    public record GoalResponse(
            short goal,
            int remainingCount
    ) {}
}
