package umc.nook.readingrooms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;

@RequiredArgsConstructor
@Controller
public class ReadingRoomSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReadingRoomService readingRoomService;

    @MessageMapping("/enter")
    public void enterRoom(ReadingRoomDTO.ReadingRoomEnterRequest dto) {
        ReadingRoomDTO.ReadingRoomEnterResponse response = readingRoomService.enterRoom(dto);

        messagingTemplate.convertAndSend("/readingroom/sub/readingroom/" + dto.getRoomId(), response);
    }

    @MessageMapping("/toggle-bgm")
    public void toggleBgm(ReadingRoomDTO.ReadingRoomBgmToggleRequest dto) {
        readingRoomService.toggleBgm(dto);
        messagingTemplate.convertAndSend("/readingroom/sub/readingroom/" + dto.getRoomId() + "/bgm-toggle", dto);
    }

}
