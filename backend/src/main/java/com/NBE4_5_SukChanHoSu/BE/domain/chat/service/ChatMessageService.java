package com.NBE4_5_SukChanHoSu.BE.domain.chat.service;

import com.NBE4_5_SukChanHoSu.BE.domain.chat.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatMessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY_PREFIX = "chat:room:";

    //메시지 저장
    public void saveMessage(ChatMessage message) {
        String key = CHAT_KEY_PREFIX + message.getRoomId();
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, Duration.ofDays(3)); // 3일 후 만료
    }

    // 메시지 내역 불러오기
    public List<ChatMessage> getMessageHistory(String roomId) {
        List<Object> rawList = redisTemplate.opsForList().range(CHAT_KEY_PREFIX + roomId, 0, -1);
        if (rawList == null || rawList.isEmpty()) return Collections.emptyList();

        return rawList.stream()
                .map(obj -> objectMapper.convertValue(obj, ChatMessage.class))
                .collect(Collectors.toList());
    }
}
