package com.NBE4_5_SukChanHoSu.BE.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
    // 메시지 타입: 입장, 채팅
    public enum MessageType{
        ENTER, TALK
    }

    private String roomId; //채팅방id
    private String sender; //보낸 사람
    private String message;
    private LocalDateTime sentAt;

}
