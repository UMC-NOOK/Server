package umc.nook.readingrooms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-rooms")
@Tag(name = "ReadingRoom API", description = "리딩룸 API")
public class ReadingRoomController {

    private final ReadingRoomService readingRoomService;

    @Operation(summary = "전체 리딩룸 목록을 조회합니다.", description = "쿼리 스트링으로 원하는 페이지를 넘겨주시면 됩니다. 페이지 번호는 0부터 시작합니다.")
    @GetMapping
    public ApiResponse<List<ReadingRoomDTO.ReadingRoomResponseDTO>> getAllReadingRooms(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(readingRoomService.getAllReadingRooms(page), SuccessCode.OK);
    }

    @Operation(summary = "사용자가 리딩룸에 가입합니다.", description = "현재 로그인한 사용자가 리딩룸에 가입합니다.")
    @PostMapping("/{roomId}/join")
    public ApiResponse<Long> joinReadingRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        Long joinedRoomId = readingRoomService.joinRoom(roomId, user);
        return ApiResponse.onSuccess(joinedRoomId, SuccessCode.OK);
    }

}
