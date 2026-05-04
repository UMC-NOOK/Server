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
import app.nook.record.service.RecordCommandService;
import app.nook.record.service.RecordQueryService;
import app.nook.record.util.RecordListCursorCodec;
import app.nook.user.annotation.CurrentUser;
import app.nook.user.domain.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Api1Version
@RequiredArgsConstructor
@RequestMapping("/records")
@Validated
public class RecordController {

    private final RecordCommandService recordCommandService;
    private final RecordQueryService recordQueryService;

    @GetMapping
    public ApiResponse<CursorResponse<BookRecordDto.BookRecordItemDto, String>> getUserRecords(
            @CurrentUser User user,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "RECENT_RECORDED") SortType order
    ) {
        CursorResponse<BookRecordDto.BookRecordItemDto, String> response =
                recordQueryService.getUserRecords(
                        user,
                        size,
                        RecordListCursorCodec.decode(cursor),
                        order
                );
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/books/{bookId}")
    public ApiResponse<CursorResponse<BookRecordDto.RecordItemDto, Long>> getBookRecords(
            @CurrentUser User user,
            @PathVariable Long bookId,
            @RequestParam(required = false) @Min(1) Long cursor,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "ALL") String emotion
    ) {
        CursorResponse<BookRecordDto.RecordItemDto, Long> response =
                recordQueryService.getBookRecords(user, bookId, size, cursor, emotion);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @GetMapping("/emotions/{bookId}")
    public ApiResponse<BookRecordDto.RecordEmotionCountResponse> getRecordEmotionCounts(
            @CurrentUser User user,
            @PathVariable Long bookId
    ) {
        return ApiResponse.onSuccess(
                recordQueryService.getRecordEmotionCounts(user,bookId),
                SuccessCode.OK
        );
    }

    @PostMapping("/books/{bookId}")
    public ApiResponse<Void> saveRecord(
            @CurrentUser User user,
            @PathVariable Long bookId,
            @Valid @RequestBody RecordRequestDto requestDto
            ) {
        recordCommandService.createRecord(user, bookId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.CREATED);
    }

    @PutMapping("/{recordId}")
    public ApiResponse<Void> updateRecord(
            @CurrentUser User user,
            @PathVariable Long recordId,
            @Valid @RequestBody RecordUpdateRequestDto requestDto
    ) {
        recordCommandService.updateRecord(user, recordId, requestDto);
        return ApiResponse.onSuccess(null, SuccessCode.OK);
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @CurrentUser User user,
            @PathVariable Long recordId
    ) {
        recordCommandService.deleteRecord(user, recordId);
        return ApiResponse.onSuccess(null, SuccessCode.NO_CONTENT);
    }

    @GetMapping("/count")
    public ApiResponse<RecordResponseDto.RecordCountDto> countRecords(
            @CurrentUser User user
    ) {
        return ApiResponse.onSuccess(
                recordCommandService.countRecords(user.getId()),
                SuccessCode.OK
        );
    }

    @GetMapping("/{recordId}")
    public ApiResponse<BookRecordDto.RecordItemDto> getRecordDetail(
            @CurrentUser User user,
            @PathVariable Long recordId
    ) {
        return ApiResponse.onSuccess(
                recordQueryService.getRecordDetail(user, recordId),
                SuccessCode.OK
        );
    }


}
