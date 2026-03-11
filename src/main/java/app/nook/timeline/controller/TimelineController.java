package app.nook.timeline.controller;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.service.TimelineQueryService;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/library/{libraryId}/timeline")
public class TimelineController {

    private final TimelineQueryService timelineQueryService;

    @GetMapping("/summary")
    public ApiResponse<TimelineResponseDto.TimelineSummaryDto> getTimelineSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Long libraryId
    ) {
        TimelineResponseDto.TimelineSummaryDto response =
                timelineQueryService.getTimelineSummary(userDetails.getUser(), libraryId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping
    public ApiResponse<TimelineResponseDto.TimelinePreviewDto> getTimelinePreview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Long libraryId,
            @RequestParam(required = false) @Min(1) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        TimelineResponseDto.TimelinePreviewDto response =
                timelineQueryService.getTimelinePreview(userDetails.getUser(), libraryId, cursor, size);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }
}
