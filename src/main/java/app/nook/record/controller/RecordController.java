package app.nook.record.controller;


import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.service.RecordService;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/books/{bookId}")
    public ApiResponse<Void> saveRecord(
            @CurrentUser User user,
            @PathVariable Long bookId,
            @Valid @RequestBody RecordRequestDto requestDto
            ) {
        recordService.createRecord(user, bookId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.CREATED);
    }

    @PutMapping("/{recordId}")
    public ApiResponse<Void> updateRecord(
            @CurrentUser User user,
            @PathVariable Long recordId,
            @Valid @RequestBody RecordUpdateRequestDto requestDto
    ) {
        recordService.updateRecord(user, recordId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @CurrentUser User user,
            @PathVariable Long recordId
    ) {
        recordService.deleteRecord(user, recordId);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @GetMapping("/count")
    public ApiResponse<RecordResponseDto.RecordCountDto> countRecords(
            @CurrentUser User user
    ) {
        return ApiResponse.onSuccess(
                recordService.countRecords(user.getId()),
                SuccessCode.OK
        );
    }

}
