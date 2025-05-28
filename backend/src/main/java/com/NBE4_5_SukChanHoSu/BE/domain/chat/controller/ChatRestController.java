package com.NBE4_5_SukChanHoSu.BE.domain.chat.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatMessage;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageService chatMessageService;

    /**
     * 채팅방에 저장된 메시지 내역 전체 조회
     * @param roomId 채팅방 ID
     * @return 해당 채팅방의 메시지 리스트
     */
    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessage> getMessageHistory(@PathVariable String roomId) {
        return chatMessageService.getMessageHistory(roomId);
    }
}
