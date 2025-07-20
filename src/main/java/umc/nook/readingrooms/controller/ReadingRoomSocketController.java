package umc.nook.readingrooms.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.ErrorCode;
import umc.nook.common.response.SuccessCode;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;
import umc.nook.users.service.CustomUserDetails;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ReadingRoomSocketController {

    private final ReadingRoomService readingRoomService;

    @MessageMapping("/enter")
    @SendTo("/sub/readingroom/user-enter")
    public void enterRoom(
            @Payload ReadingRoomDTO.ReadingRoomEnterRequest request) {
        readingRoomService.enterRoom(request);
    }

    @MessageMapping("/toggle-bgm")
    public void handleToggleBgm(
            @Payload ReadingRoomDTO.ReadingRoomBgmToggleRequest dto) {
        readingRoomService.toggleBgm(dto);
    }

    @MessageMapping("/leave")
    public void leaveRoom(
            @Payload ReadingRoomDTO.ReadingRoomLeaveRequest dto) {
        readingRoomService.leaveRoom(dto);
    }
}
