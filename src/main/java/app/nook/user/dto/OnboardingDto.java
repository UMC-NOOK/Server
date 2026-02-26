package app.nook.user.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class OnboardingDto {

    public record CompleteRequest(
            @Min(1) @Max(300) short goal,
            @NotBlank @Size(max = 10) String nickname,
            @NotEmpty @Size(min = 1, max = 2) List<String> categories,
            String profileUrl
    ) {}

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

    public record ProfileImageUploadResponse(String profileUrl) {}
}
