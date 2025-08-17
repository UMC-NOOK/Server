package umc.nook.readingrooms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.readingrooms.domain.HashtagName;
import umc.nook.readingrooms.domain.ThemeName;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-rooms")
@Tag(name = "ReadingRoom API", description = "리딩룸 API")
public class ReadingRoomController {

    private final ReadingRoomService readingRoomService;

    @Operation(summary = "전체 리딩룸 조회", description = "쿼리 스트링으로 원하는 페이지를 넘겨주시면 됩니다. 페이지 번호는 0부터 시작합니다.")
    @GetMapping
    public ApiResponse<List<ReadingRoomDTO.ReadingRoomResponseDTO>> getAllReadingRooms(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(readingRoomService.getAllReadingRooms(page), SuccessCode.OK);
    }

    @Operation(summary = "사용자가 가입한 리딩룸 조회")
    @GetMapping("/join")
    public ApiResponse<List<ReadingRoomDTO.ReadingRoomResponseDTO>> joinedReadingRoom(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.onSuccess(readingRoomService.getJoinedReadingRooms(user),SuccessCode.OK);
    }

    @Operation(summary = "리딩룸 가입", description = "가입한 리딩룸 ID를 반환합니다.")
    @PostMapping("/{roomId}/join")
    public ApiResponse<Long> joinReadingRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        Long joinedRoomId = readingRoomService.joinRoom(roomId, user);
        return ApiResponse.onSuccess(joinedRoomId, SuccessCode.CREATED);
    }

    @Operation(summary = "리딩룸 생성", description = "생성한 리딩룸 ID를 반환합니다. 생성한 사용자가 호스트가 됩니다.")
    @PostMapping
    public ApiResponse<Long> createReadingRoom(@RequestParam ThemeName themeName, @RequestParam List<HashtagName> hashtags, @RequestBody ReadingRoomDTO.ReadingRoomRequestDTO readingRoomRequestDTO, @AuthenticationPrincipal CustomUserDetails user) {
        Long createdRoomId = readingRoomService.createRoom(readingRoomRequestDTO, themeName, hashtags, user);
        return ApiResponse.onSuccess(createdRoomId, SuccessCode.CREATED);
    }

    @Operation(summary = "호스트가 리딩룸 삭제")
    @DeleteMapping("/{roomId}/host")
    public ApiResponse<Long> deleteReadingRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        readingRoomService.deleteRoom(roomId, user);
        return ApiResponse.onSuccess(roomId, SuccessCode.NO_CONTENT);
    }

    @Operation(summary = "게스트가 가입한 리딩룸 탈퇴")
    @DeleteMapping("/{roomId}/guest")
    public ApiResponse<Long> leaveReadingRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        readingRoomService.leaveRoom(roomId, user);
        return ApiResponse.onSuccess(roomId, SuccessCode.NO_CONTENT);
    }

    @Operation(summary = "호스트가 리딩룸 수정")
    @PatchMapping("/{roomId}")
    public ApiResponse<Long> updateReadingRoom(@PathVariable Long roomId, @RequestParam ThemeName themeName, @RequestParam List<HashtagName> hashtags, @RequestBody ReadingRoomDTO.ReadingRoomRequestDTO dto, @AuthenticationPrincipal CustomUserDetails user) {
        readingRoomService.updateRoom(roomId, dto, themeName, hashtags, user);
        return ApiResponse.onSuccess(roomId, SuccessCode.OK);
    }

    @Operation(summary = "사용자가 리딩룸의 호스트인지 게스트인지 확인")
    @GetMapping("/{roomId}/my-role")
    public ApiResponse<String> getMyRoleInRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        String role = readingRoomService.getUserRoleInRoom(roomId, user);
        return ApiResponse.onSuccess(role, SuccessCode.OK);
    }

    @Operation(summary = "독서중인 책 제목 조회")
    @GetMapping("/reading")
    public ApiResponse<List<ReadingRoomDTO.ReadingBookRequest>> getReadingBooksInRoom(@AuthenticationPrincipal CustomUserDetails user) {
        List<ReadingRoomDTO.ReadingBookRequest> name = readingRoomService.getReadingBooksInRoom(user);
        return ApiResponse.onSuccess(name, SuccessCode.OK);
    }

    @Operation(summary = "리딩룸에 가입한 사용자 이름과 호스트/게스트 정보 조회")
    @GetMapping("/{roomId}/joined-users")
    public ApiResponse<List<ReadingRoomDTO.JoinedUsersResponseDTO>> getJoinedUsers(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails user) {
        List<ReadingRoomDTO.JoinedUsersResponseDTO> name = readingRoomService.getJoinedUsersInRoom(roomId, user);
        return ApiResponse.onSuccess(name, SuccessCode.OK);
    }

    @Operation(summary = "사용자가 최근 접속한 리딩룸 1개를 조회")
    @GetMapping("/last-accessed")
    public ApiResponse<ReadingRoomDTO.LastAccessedReadingRoomResponseDTO> getLastAccessedRoom(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.onSuccess(
                readingRoomService.getLastAccessedRoom(user.getUser()),
                SuccessCode.OK
        );
    }

    @Operation(summary = "리딩룸의 테마와 bgm 조회")
    @GetMapping("/{roomId}/themes")
    public ApiResponse<ReadingRoomDTO.ReadingRoomThemeResponseDTO> getTheme(@PathVariable Long roomId) {
        return ApiResponse.onSuccess(readingRoomService.getRoomTheme(roomId), SuccessCode.OK);
    }
}
