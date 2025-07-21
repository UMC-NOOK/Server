package umc.nook.records.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.records.dto.ChatDTO;
import umc.nook.records.dto.RecordDTO;
import umc.nook.records.service.RecordService;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records")
@Tag(name = "Book Record API", description = "독서 기록 API")
public class RecordController {

    private final RecordService recordService;

    @Operation(
            summary = "눅톡 메시지 입력",
            description = "책에 대한 감상문 작성을 도와주는 GPT 챗봇과의 대화 메시지를 전송합니다. GPT 응답도 함께 저장됩니다."
    )
    @PostMapping("/send-message")
    public ApiResponse<ChatDTO.ChatResponseDTO> chatWithGpt(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = true)
            ChatDTO.ChatRequestDTO requestDTO
    ) throws JsonProcessingException {
        ChatDTO.ChatResponseDTO response = recordService.saveChatMessage(userDetails.getUser(), requestDTO);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(
            summary = "독서 문장 기록",
            description = "독서 중 인상 깊은 문장을 기록합니다."
    )
    @PostMapping("/sentence/save")
    public ApiResponse<String> saveSentence(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated
            RecordDTO.RecordRequestDTO requestDTO
    ) {
        recordService.saveSentence(userDetails.getUser(), requestDTO);
        return ApiResponse.onSuccess("문장 기록이 저장되었습니다.", SuccessCode.OK);
    }

    @Operation(
            summary = "독서 문장 수정",
            description = "기록한 독서 문장의 페이지 번호와 내용을 수정합니다."
    )
    @PutMapping("/sentence/{recordId}")
    public ApiResponse<RecordDTO.SentenceResponseDTO> updateSentence(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated RecordDTO.RecordUpdateRequestDTO updateRequestDTO
    ) {
        RecordDTO.SentenceResponseDTO response = recordService.updateSentence(userDetails.getUser(), updateRequestDTO);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(
            summary = "독서 문장 삭제",
            description = "기록한 독서 문장과 그에 달린 모든 댓글을 삭제합니다."
    )
    @DeleteMapping("/sentence/{recordId}")
    public ApiResponse<String> deleteSentence(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId
    ) {
        recordService.deleteRecord(userDetails.getUser(), recordId);
        return ApiResponse.onSuccess("문장이 삭제되었습니다.", SuccessCode.OK);
    }

    @Operation(
            summary = "감상 등록",
            description = "기록한 독서 문장에 대한 감상을 댓글 형식으로 저장합니다."
    )
    @PostMapping("/comment/save")
    public ApiResponse<RecordDTO.CommentResponseDTO> saveCommentary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated RecordDTO.CommentRequestDTO requestDTO
    ) {
        RecordDTO.CommentResponseDTO response = recordService.saveCommentary(userDetails.getUser(), requestDTO);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(
            summary = "감상 수정",
            description = "기록된 감상의 내용을 수정합니다."
    )
    @PutMapping("/comment/{commentId}")
    public ApiResponse<RecordDTO.CommentResponseDTO> updateCommentary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody @Validated RecordDTO.CommentUpdateRequestDTO updateRequestDTO
    ) {
        RecordDTO.CommentResponseDTO response = recordService.updateComment(userDetails.getUser(), commentId, updateRequestDTO);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(
            summary = "감상 삭제",
            description = "기록된 감상을 삭제합니다."
    )
    @DeleteMapping("/comment/{commentId}")
    public ApiResponse<String> deleteCommentary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId
    ) {
        recordService.deleteComment(userDetails.getUser(), commentId);
        return ApiResponse.onSuccess("감상이 삭제되었습니다.", SuccessCode.OK);
    }

    @Operation(
            summary = "눅톡 대화 기록 조회",
            description = "눅톡 기록을 시간순으로 조회합니다."
    )
    @GetMapping("/chat/view")
    public ApiResponse<List<ChatDTO.ChatResponseDTO>> viewChatMessages(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "책 ID", required = true)
            @RequestParam Long bookId
    ) {
        List<ChatDTO.ChatResponseDTO> response = recordService.viewChatMessages(userDetails.getUser(), bookId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }

    @Operation(
            summary = "기록된 문장/감상 목록 조회",
            description = "특정 책에 대해 기록된 독서 문장(감상 전 본문 문장들)을 시간순으로 조회합니다."
    )
    @GetMapping("/sentence/list")
    public ApiResponse<List<RecordDTO.RecordResponseDTO>> viewSentences(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long bookId
    ) {
        List<RecordDTO.RecordResponseDTO> response =
                recordService.viewRecordsByBookId(userDetails.getUser(), bookId);
        return ApiResponse.onSuccess(response, SuccessCode.OK);
    }


}
