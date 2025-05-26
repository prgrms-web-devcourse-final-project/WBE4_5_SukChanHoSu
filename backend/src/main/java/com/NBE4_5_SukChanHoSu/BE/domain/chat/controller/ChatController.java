package com.NBE4_5_SukChanHoSu.BE.domain.chat.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatMessage;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatRoom;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.NotificationMessage;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.service.ChatRoomService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserErrorCode;
import com.NBE4_5_SukChanHoSu.BE.global.exception.security.BadCredentialsException;
import com.NBE4_5_SukChanHoSu.BE.global.security.PrincipalDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import com.NBE4_5_SukChanHoSu.BE.domain.chat.service.ChatMessageService;
import java.time.LocalDateTime;

import java.security.Principal;


@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {

        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) {
            throw new BadCredentialsException(
                    UserErrorCode.USER_UNAUTHORIZED.getCode(),
                    UserErrorCode.USER_UNAUTHORIZED.getMessage()
            );
        }

        Object principalObj = token.getPrincipal();
        if (!(principalObj instanceof PrincipalDetails details)) {
            throw new BadCredentialsException(
                    UserErrorCode.USER_UNAUTHORIZED.getCode(),
                    UserErrorCode.USER_UNAUTHORIZED.getMessage()
            );
        }


        String nickname = details.getUser().getUserProfile().getNickName(); // or getUser().getNickname() if 존재
        message.setSender(nickname);

        //현재 시간세팅
        message.setSentAt(LocalDateTime.now());
        // redis 저장
        chatMessageService.saveMessage(message);

        //채팅방 상태 갱신
        ChatRoom room = chatRoomService.findRoomById(message.getRoomId());
        if (room != null) {
            room.setLastMessage(message.getMessage());                         // 최근 메시지
            room.setLastMessageTime(message.getSentAt().toString());           // ISO 문자열
            room.setUnread(true);                                              // 새 메시지 도착 표시
            chatRoomService.saveRoom(room);                                    // 업데이트 저장
        }

        //알림 Websocket 전송(상대방에게)
        String receiver = room.getSender().equals(nickname) ? room.getReceiver() : room.getSender();

        NotificationMessage notification = NotificationMessage.builder()
                .roomId(room.getRoomId())
                .sender(nickname)
                .receiver(receiver)
                .message(message.getMessage())
                .type("NEW_MESSAGE")
                .build();

        messagingTemplate.convertAndSend("/sub/notify/" + receiver, notification);

        //새로운 실시간 목록 갱신 채널로도 전송
        messagingTemplate.convertAndSend("/sub/chatroom/update/" + room.getRoomId(), notification);


        //메시지 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }

}
