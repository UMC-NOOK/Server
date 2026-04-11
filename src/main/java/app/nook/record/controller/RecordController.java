package app.nook.record.controller;


import app.nook.global.dto.CursorResponse;
import app.nook.global.api.Api1Version;
import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordRequestDto;
import app.nook.record.dto.RecordResponseDto;
import app.nook.record.dto.RecordUpdateRequestDto;
import app.nook.record.service.RecordService;
import app.nook.record.service.RecordViewService;
import app.nook.record.util.RecordListCursorCodec;
import app.nook.user.service.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/api/records")
@Validated
public class RecordController {

    private final RecordService recordService;
    private final RecordViewService recordViewService;

    @GetMapping
    public ApiResponse<CursorResponse<BookRecordDto.BookRecordItemDto, String>> getUserRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "RECENT_RECORDED") SortType order
    ) {
        CursorResponse<BookRecordDto.BookRecordItemDto, String> response =
                recordViewService.getUserRecords(
                        userDetails.getUser(),
                        size,
                        RecordListCursorCodec.decode(cursor),
                        order
                );

        if (response.getItems().isEmpty()) {
            return ApiResponse.onSuccess(null, SuccessCode.NO_CONTENT);
        }

        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/books/{bookId}")
    public ApiResponse<CursorResponse<BookRecordDto.RecordItemDto, Long>> getBookRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookId,
            @RequestParam(required = false) @Min(0) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "ALL") String emotion
    ) {
        CursorResponse<BookRecordDto.RecordItemDto, Long> response =
                recordViewService.getBookRecords(userDetails.getUser(), bookId, size, cursor, emotion);

        if (response.getItems().isEmpty()) {
            return ApiResponse.onSuccess(null, SuccessCode.NO_CONTENT);
        }

        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/emotions")
    public ApiResponse<BookRecordDto.RecordEmotionCountResponse> getRecordEmotionCounts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                recordViewService.getRecordEmotionCounts(userDetails.getUser()),
                SuccessCode.OK
        );
    }

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
