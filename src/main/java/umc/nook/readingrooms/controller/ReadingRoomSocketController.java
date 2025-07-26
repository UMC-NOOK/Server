package umc.nook.readingrooms.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import umc.nook.readingrooms.domain.ReadingRoom;
import umc.nook.readingrooms.dto.ReadingRoomDTO;
import umc.nook.readingrooms.service.ReadingRoomService;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ReadingRoomSocketController {

    private final ReadingRoomService readingRoomService;

    @MessageMapping("/enter")
    public void enterRoom(
            @Payload ReadingRoomDTO.ReadingRoomEnterRequest request) {
        readingRoomService.enterRoom(request);
    }

    @MessageMapping("/bgm-toggle")
    public void handleToggleBgm(
            @Payload ReadingRoomDTO.ReadingRoomBgmToggleRequest dto) {
        readingRoomService.toggleBgm(dto);
    }

    @MessageMapping("/leave")
    public void leaveRoom(
            @Payload ReadingRoomDTO.ReadingRoomLeaveRequest dto) {
        readingRoomService.leaveRoom(dto);
    }

    @MessageMapping("/reading-books")
    public void readingBooks(
            @Payload ReadingRoomDTO.ReadingBookPayload payload) {
        readingRoomService.readingBooks(payload);
    }
}
