package com.NBE4_5_SukChanHoSu.BE.domain.chat.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatRoom;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // 채팅방 목록 조회
    @GetMapping
    public List<ChatRoom> findAllRooms() {
        return chatRoomService.findAllRooms();
    }

    // 채팅방 단일 조회
    @GetMapping("/{roomId}")
    public ChatRoom findRoomById(@PathVariable String roomId) {
        return chatRoomService.findRoomById(roomId);
    }

    // 채팅방 생성 매칭시 roomId 생성
    @PostMapping
    public ChatRoom createRoom(@RequestParam String sender, @RequestParam String receiver) {
        return chatRoomService.createRoom(sender, receiver);
    }


}
