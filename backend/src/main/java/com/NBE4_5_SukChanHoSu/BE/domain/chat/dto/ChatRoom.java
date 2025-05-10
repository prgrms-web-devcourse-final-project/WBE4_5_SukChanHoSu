package com.NBE4_5_SukChanHoSu.BE.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ChatRoom implements Serializable {
    private String roomId;
    private String sender;
    private String receiver;

    @Builder
    public ChatRoom(String roomId, String sender, String receiver) {
        this.roomId = roomId;
        this.sender = sender;
        this.receiver = receiver;
    }
}



