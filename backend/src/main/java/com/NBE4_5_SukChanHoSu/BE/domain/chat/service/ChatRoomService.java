package com.NBE4_5_SukChanHoSu.BE.domain.chat.service;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatRoom;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatRoomService {

    private static final String CHAT_ROOMS = "CHAT_ROOM";

    @Resource(name = "redisTemplate")
    private HashOperations<String, String, ChatRoom> hashOps;

    // 채팅방 생성 or 존재시 반환
    public ChatRoom createRoom(String sender, String receiver) {
        for (ChatRoom room : findAllRooms()) {
            boolean matched = (room.getSender().equals(sender) && room.getReceiver().equals(receiver)) ||
                    (room.getSender().equals(receiver) && room.getReceiver().equals(sender));
            if (matched) return room;
        }

        String roomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .sender(sender)
                .receiver(receiver)
                .build();

        hashOps.put(CHAT_ROOMS, roomId, chatRoom);
        return chatRoom;
    }

    // 채팅방 조회
    public ChatRoom findRoomById(String roomId) {
        return hashOps.get(CHAT_ROOMS, roomId);
    }

    // 모든 채팅방 목록 조회
    public List<ChatRoom> findAllRooms() {
        return new ArrayList<>(hashOps.entries(CHAT_ROOMS).values());
    }
}
