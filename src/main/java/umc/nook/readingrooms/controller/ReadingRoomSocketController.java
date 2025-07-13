package umc.nook.readingrooms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;
import umc.nook.users.service.CustomUserDetails;

@RequiredArgsConstructor
@Controller
public class ReadingRoomSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReadingRoomService readingRoomService;

    @MessageMapping("/enter")
    public void enterRoom(ReadingRoomDTO.ReadingRoomEnterRequest dto,
                          @AuthenticationPrincipal CustomUserDetails userPrincipal) {
        dto.setUserId(userPrincipal.getUser().getUserId());

        ReadingRoomDTO.ReadingRoomEnterResponse response = readingRoomService.enterRoom(dto);
        messagingTemplate.convertAndSend("/readingroom/sub/readingroom/" + dto.getRoomId(), response);
    }

    @MessageMapping("/toggle-bgm")
    public void toggleBgm(@Payload ReadingRoomDTO.ReadingRoomBgmToggleRequest dto,
                          @AuthenticationPrincipal CustomUserDetails userPrincipal) {
        dto.setUserId(userPrincipal.getUser().getUserId());
        readingRoomService.toggleBgm(dto);
        messagingTemplate.convertAndSend("/sub/readingroom/" + dto.getRoomId() + "/bgm-toggle", dto);
    }
}
