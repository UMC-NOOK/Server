package umc.nook.readingrooms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.nook.common.response.ApiResponse;
import umc.nook.readingrooms.service.ReadingRoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-rooms")
@Tag(name = "ReadingRoom API", description = "리딩룸 API")
public class ReadingRoomController {

}
