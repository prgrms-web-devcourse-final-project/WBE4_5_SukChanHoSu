package com.NBE4_5_SukChanHoSu.BE.domain.chat.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatMessage;

import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserErrorCode;
import com.NBE4_5_SukChanHoSu.BE.global.exception.security.BadCredentialsException;
import com.NBE4_5_SukChanHoSu.BE.global.security.PrincipalDetails;
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
        //메시지 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}
