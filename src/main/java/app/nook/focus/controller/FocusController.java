package app.nook.focus.controller;

import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.service.ThemeService;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/focuses")
public class FocusController {

    private final ThemeService themeService;

    @GetMapping("/themes")
    public ApiResponse<FocusResponseDto.ThemeListDto> getThemes() {
        return ApiResponse.onSuccess(
                themeService.getThemes(),
                SuccessCode.OK
        );
    }
}

