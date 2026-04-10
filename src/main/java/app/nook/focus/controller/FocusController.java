package app.nook.focus.controller;

import app.nook.global.api.Api1Version;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.FocusService;
import app.nook.focus.service.ThemeService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/focuses")
public class FocusController {

    private final ThemeService themeService;
    private final FocusService focusService;

    @GetMapping("/themes")
    public ApiResponse<FocusResponseDto.ThemeListDto> getThemes() {
        return ApiResponse.onSuccess(
                themeService.getThemes(),
                SuccessCode.OK
        );
    }

    @PostMapping("/start")
    public ApiResponse<FocusResponseDto.FocusStart> startFocus(
            @CurrentUser User user,
            @RequestBody @Valid FocusRequestDto.FocusStart request
    ) {
        return ApiResponse.onSuccess(focusService.startFocus(user, request), SuccessCode.CREATED);
    }

    @PostMapping("/end")
    public ApiResponse<FocusResponseDto.FocusEnd> endFocus(
            @CurrentUser User user,
            @RequestBody @Valid FocusRequestDto.FocusEnd request
    ) {
        return ApiResponse.onSuccess(focusService.endFocus(user.getId(), request), SuccessCode.OK);
    }
}
