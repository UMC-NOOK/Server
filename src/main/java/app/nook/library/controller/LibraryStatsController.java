package app.nook.library.controller;

import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.library.dto.FocusRankDto;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryStatsService;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/library/stats")
@Validated
public class LibraryStatsController {

    private final LibraryStatsService libraryStatsService;

    // 서재 월별 포커스 통계 조회
    @GetMapping("/monthly")
    public ApiResponse<FocusRankDto.MonthlyBooksResponseDto> viewMonthly(
            @CurrentUser User user,
            @RequestParam
            @NotNull
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth yearMonth
    ) {
        FocusRankDto.MonthlyBooksResponseDto response =
                libraryStatsService.viewMonthly(user.getId(), yearMonth);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    // 서재 월별 포커스 시간 통계 조회
    @GetMapping("/focus-monthly")
    public ApiResponse<FocusRankDto.FocusBookResponseDto> viewFocusTimeStats(
            @CurrentUser User user,
            @RequestParam
            @NotNull
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth yearMonth
    ) {
        FocusRankDto.FocusBookResponseDto response =
                libraryStatsService.viewFocusTimeStats(user.getId(), yearMonth);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }


}
