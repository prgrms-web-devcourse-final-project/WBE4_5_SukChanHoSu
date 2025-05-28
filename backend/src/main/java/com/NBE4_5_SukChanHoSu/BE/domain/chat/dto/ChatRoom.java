package com.NBE4_5_SukChanHoSu.BE.domain.chat.dto;

import lombok.*;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom implements Serializable {
    private String roomId;
    private String sender;
    private String receiver;
    private String lastMessage;
    private String lastMessageTime;
    private boolean unread; // 새 메시지 true: 안 읽음
}



