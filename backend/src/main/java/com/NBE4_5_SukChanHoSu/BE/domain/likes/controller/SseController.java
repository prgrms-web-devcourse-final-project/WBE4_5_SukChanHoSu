package com.NBE4_5_SukChanHoSu.BE.domain.likes.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.NotificationEvent;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.Ut;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sse")
@Tag(name = "SSE", description = "이벤트 전송")
@RequiredArgsConstructor
public class SseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(SseController.class);
    private final Ut ut;

    // 애플리케이션 종료시 SSE 연결 해제
    public void destroy() {
        emitters.forEach((userId, emitter) -> {
            emitter.complete();
            logger.info("애플리케이션 종료 시 SSE 연결 종료 - userId: {}", userId);
        });
        emitters.clear();
    }


    @EventListener  // 이벤트 발행시 탐지
    public void handleNotification(NotificationEvent event) {
        Long userId = event.getUserId();
        String message = event.getMessage();
        logger.info("이벤트 수신 - userId: {}, message: {}", userId, message);

        // SSE를 통해 알림 전송
        sendNotification(userId, message);
    }

    // SSE 연결 생성
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 연결 생성", description = "타임 아웃 x")
    public SseEmitter createConnection() {
        Long userId = ut.getUserProfileByContextHolder().getUserId();

        // todo: 리소스 낭비가 심한거같은데 개선방안 고민
        SseEmitter emitter = new SseEmitter(-1L);
        emitters.put(userId, emitter);
        logger.info("SSE 연결 생성 - userId: {}", userId);

//        // 연결 종료시
//        emitter.onCompletion(() -> {
//            logger.info("SSE 연결 종료 - userId: {}", userId);
//            emitters.remove(userId);
//        });
//        emitter.onTimeout(() -> {
//            logger.info("SSE 연결 타임아웃 - userId: {}", userId);
//            emitters.remove(userId);
//        });

        return emitter;
    }

    // 알림 전송
    public void sendNotification(Long userId, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null){
           try{
               logger.info("알림 전송 - userId: {}, message: {}", userId, message);
               emitter.send(SseEmitter.event().data(message));
           } catch (Exception e) {
               logger.error("알림 전송 실패 - userId: {}, message: {}", userId, message, e);
               emitter.complete();
               emitters.remove(userId);
           }
        }
    }

    public Map<Long, SseEmitter> getEmitters() {
        return emitters;
    }


}
