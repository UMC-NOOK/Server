package app.nook.timeline.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.timeline.dto.TimelineResponseDto;
import app.nook.timeline.service.TimelineQueryService;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@Validated
@RequestMapping("/library/{libraryId}/timeline")
public class TimelineController {

    private final TimelineQueryService timelineQueryService;

    @GetMapping("/summary")
    public ApiResponse<TimelineResponseDto.TimelineSummaryDto> getTimelineSummary(
            @CurrentUser User user,
            @PathVariable @Positive Long libraryId
    ) {
        TimelineResponseDto.TimelineSummaryDto response =
                timelineQueryService.getTimelineSummary(user, libraryId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping
    public ApiResponse<TimelineResponseDto.TimelinePreviewDto> getTimelinePreview(
            @CurrentUser User user,
            @PathVariable @Positive Long libraryId
    ) {
        TimelineResponseDto.TimelinePreviewDto response =
                timelineQueryService.getTimelinePreview(user, libraryId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/{timelineId}")
    public ApiResponse<TimelineResponseDto.TimelineDetailDto> getTimelineDetail(
            @CurrentUser User user,
            @PathVariable @Positive Long libraryId,
            @PathVariable @Positive Long timelineId
    ) {
        TimelineResponseDto.TimelineDetailDto response =
                timelineQueryService.getTimelineDetail(user, libraryId, timelineId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }
}
