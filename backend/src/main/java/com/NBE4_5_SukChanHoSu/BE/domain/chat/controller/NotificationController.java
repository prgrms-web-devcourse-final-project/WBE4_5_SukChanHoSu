package com.NBE4_5_SukChanHoSu.BE.domain.chat.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatRoom;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.NotificationMessage;
import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import javax.management.Notification;


@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/notice")
    public void sendNotification(@RequestBody NotificationMessage message) {
        // WebSocket 구독자에게 전송
        messagingTemplate.convertAndSend("/sub/chatroom/update/" + message.getRoomId(), message);


    }
}
