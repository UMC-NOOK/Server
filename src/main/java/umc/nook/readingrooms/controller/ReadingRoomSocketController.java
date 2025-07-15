package umc.nook.readingrooms.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;
import umc.nook.users.service.CustomUserDetails;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ReadingRoomSocketController {

    private final ReadingRoomService readingRoomService;

    @MessageMapping("/enter")
    public void enterRoom(ReadingRoomDTO.ReadingRoomEnterRequest dto,
                          @AuthenticationPrincipal CustomUserDetails userPrincipal) {
        dto.setUserId(userPrincipal.getUser().getUserId());
        log.info("WebSocket: User {} requesting to enter room {}", dto.getUserId(), dto.getRoomId());
        readingRoomService.enterRoom(dto);
    }

    @MessageMapping("/pub/toggle-bgm")
    public void handleToggleBgm(@Payload ReadingRoomDTO.ReadingRoomBgmToggleRequest dto,
                                @AuthenticationPrincipal CustomUserDetails userPrincipal) {
        dto.setUserId(userPrincipal.getUser().getUserId());
        log.info("WebSocket: User {} toggling BGM in room {}: {}", dto.getUserId(), dto.getRoomId(), dto.isBgmOn());
        readingRoomService.toggleBgm(dto);
    }
}
