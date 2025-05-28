package com.NBE4_5_SukChanHoSu.BE.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {
    private String roomId;
    private String sender;
    private String receiver;
    private String message; // 메시지 내용
    private String type; // 새 메시지
}
