package app.nook.record.controller;


import app.nook.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.service.RecordService;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/books/{bookId}")
    public ApiResponse<Void> saveRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookId,
            @Valid @RequestBody RecordRequestDto requestDto
            ) {
        recordService.createRecord(userDetails.getUser(), bookId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.CREATED);
    }

    @PutMapping("/{recordId}")
    public ApiResponse<Void> updateRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @Valid @RequestBody RecordUpdateRequestDto requestDto
    ) {
        recordService.updateRecord(userDetails.getUser(), recordId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId
    ) {
        recordService.deleteRecord(userDetails.getUser(), recordId);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @GetMapping("/count")
    public ApiResponse<RecordResponseDto.RecordCountDto> countRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                recordService.countRecords(userDetails.getUser().getId()),
                SuccessCode.OK
        );
    }

}
