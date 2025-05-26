package com.NBE4_5_SukChanHoSu.BE.domain.likes.service;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.NotificationEvent;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Gender;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.global.exception.redis.RedisSerializationException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.UserNotFoundException;
import com.NBE4_5_SukChanHoSu.BE.global.util.DateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LettuceConnectionFactory lettuceConnectionFactory; // Redis 연결 관리
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행

    private static final Logger logger = LoggerFactory.getLogger(NoticeService.class);
    private static final String LIKE_STREAM = "like";
    private static final String MATCHING_STREAM = "matching";
    private final ConcurrentHashMap<Long, List<Map<String, String>>> notifications = new ConcurrentHashMap<>();
    private volatile boolean running = true; // 스레드 실행 상태 플래그

    private String lastLikeId = "0-0"; // 초기값
    private String lastMatchId = "0-0"; // 초기값

    // 스트림 중지 메서드
    public void stop() {
        this.running = false; // running 플래그를 false로 설정
    }


    // Like Stream 리스너 시작
    public void startLikeStreamListener() {
        while (running) { // 앱이 작동중일때만 실행
            try {
                // redis 연결이 끊어졌는지 확인
                if (!lettuceConnectionFactory.isRunning()) {
                    // 재연결
                    lettuceConnectionFactory.start();
                }
                // 스트림 읽고 처리
                else {
                    processStream(LIKE_STREAM, lastLikeId, this::processLikeEvent);
                }
            } catch (Exception e) {
                if (!running) {
                    break; // running flag가 false면 스레드 종료
                }
                logger.error("Like Stream 처리 중 오류 발생: ", e);
                break;
            }
        }
    }

    // Match Stream 리스너 시작
    public void startMatchStreamListener() {
        while (running) {
            try {
                if (!lettuceConnectionFactory.isRunning()) {
                    lettuceConnectionFactory.start(); // 재시작
                }
                // 스트림 읽고 처리
                else{
                    processStream(MATCHING_STREAM,lastMatchId,this::processMatchingEvent);
                }
            } catch (Exception e) {
                if (!running) {
                    break; // running flag가 false면 스레드 종료
                }
                logger.error("Match Stream 처리 중 오류 발생: ", e);
                break;
            }
        }
    }

    private void processStream(String streamName,String lastId, Consumer<MapRecord<String,Object,Object>> processEvent) {
//        StreamReadOptions options = StreamReadOptions.empty().count(1);
//        // 마지막으로 읽은 Id 부터 읽음
//        StreamOffset<String> offset = StreamOffset.create(LIKE_STREAM, ReadOffset.from(lastLikeId));
//        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(options, offset);

        // 블로킹 읽기 옵션 설정 (10초 대기)
        StreamReadOptions options = StreamReadOptions.empty().block(Duration.ofSeconds(10)).count(1);
        // 마지막으로 읽은 ID부터 읽기
        StreamOffset<String> offset = StreamOffset.create(streamName, ReadOffset.from(lastId));
        // Redis Stream에서 데이터 읽기
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(options, offset);

        if (!records.isEmpty()) {
            for (MapRecord<String, Object, Object> record : records) { // record = ID:Content
                // record를 인자로 processEvent(processMatchingEvnet) 호출
                processEvent.accept(record);
            }
        }
    }

//    // WebSocket을 통해 알림 전송(경로: /sub/notifications/{userId})
//    private void sendNotification(Long userId, Map<String, String> notification) {
//        messagingTemplate.convertAndSend("/sub/notifications/" + userId, notification.get("message"));
//        logger.info("메시지: {}", notification.get("message"));
//    }

    // 알림 내역 저장
    private void saveNotification(Long userId, Map<String, String> notification) {
        notifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
    }

    // 알림 목록 조회, 시간 변환
    public List<Map<String, String>> getNotifications(Long userId) {
        List<Map<String, String>> updateNotifications = notifications.getOrDefault(userId, new ArrayList<>());
        List<Map<String, String>> result = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Map<String, String> notification : updateNotifications) {
            Map<String, String> updated = new HashMap<>();
            try {
                String timeStr = notification.get("time");
                LocalDateTime ldt = LocalDateTime.parse(timeStr, formatter);
                Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                Date time = Date.from(instant);

                updated.put("message", notification.get("message"));
                updated.put("timeAgo", DateUtils.getTimeAgo(time));  // 기존 로직 사용
                result.add(updated);
            } catch (DateTimeParseException e) {
                System.err.println("날짜 파싱 실패: " + notification.get("time"));
            }
        }
        return result;
    }

    // 알림 읽음 처리
    public void markAsRead(Long userId) {
        notifications.remove(userId);
        deleteLikeStreamFromRedis(userId);
        deleteMatchingStreamFromRedis(userId);
    }

    // 미확인 알림 개수 조회
    public int getUnreadNotificationCount(Long userId) {
        return notifications.getOrDefault(userId, new ArrayList<>()).size();
    }

    // 레디스에서 삭제(라이크 이벤트)
    private void deleteLikeStreamFromRedis(Long userId) {
        StreamReadOptions options = StreamReadOptions.empty().count(100); // 100개씩
        StreamOffset<String> offset = StreamOffset.create(LIKE_STREAM, ReadOffset.from("0-0")); // 처음부터
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(options, offset);

        if (!records.isEmpty()) {
            for (MapRecord<String, Object, Object> record : records) { // record = ID:Content
                Map<Object, Object> value = record.getValue();  // value = Content
                String jsonEvent = (String) value.get("data"); // Content에서 data(Json 문자열) 추출

                try {
                    Map<String, String> event = objectMapper.readValue(jsonEvent, Map.class);
                    Long toUserId = Long.valueOf(event.get("toUserId"));

                    if(userId.equals(toUserId)) {
                        // redisStream에서 삭제
                        redisTemplate.opsForStream().delete(LIKE_STREAM,record.getId().getValue());
                    }
                } catch (Exception e) {
                    throw new RedisSerializationException("500", "JSON 역 직렬화 실패");
                }
            }
        }
    }

    // 레디스에서 삭제(매칭)
    private void deleteMatchingStreamFromRedis(Long userId) {
        StreamReadOptions options = StreamReadOptions.empty().count(100); // 100개씩
        StreamOffset<String> offset = StreamOffset.create(MATCHING_STREAM, ReadOffset.from("0-0")); // 처음부터
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(options, offset);
        if (!records.isEmpty()) {
            // 남자
            if(isMale(userId)) {
                for (MapRecord<String, Object, Object> record : records) { // record = ID:Content
                    Map<Object, Object> value = record.getValue();  // value = Content
                    String jsonEvent = (String) value.get("data"); // Content에서 data(Json 문자열) 추출

                    try {
                        Map<String, String> event = objectMapper.readValue(jsonEvent, Map.class);
                        Long maleUserId = Long.valueOf(event.get("maleUserId"));

                        if(userId.equals(maleUserId)) {
                            // redisStream에서 삭제
                            redisTemplate.opsForStream().delete(MATCHING_STREAM,record.getId().getValue());
                        }
                    } catch (Exception e) {
                        throw new RedisSerializationException("500", "JSON 역 직렬화 실패");
                    }

                }
            } else{
                // 여자
                for (MapRecord<String, Object, Object> record : records) { // record = ID:Content
                    Map<Object, Object> value = record.getValue();  // value = Content
                    String jsonEvent = (String) value.get("data"); // Content에서 data(Json 문자열) 추출

                    try {
                        Map<String, String> event = objectMapper.readValue(jsonEvent, Map.class);
                        Long femaleUserId = Long.valueOf(event.get("femaleUserId"));

                        if(userId.equals(femaleUserId)) {
                            // redisStream에서 삭제
                            redisTemplate.opsForStream().delete(MATCHING_STREAM,record.getId().getValue());
                        }
                    } catch (Exception e) {
                        throw new RedisSerializationException("500", "JSON 역 직렬화 실패");
                    }
                }
            }
        }
    }

    boolean isMale(Long userId) {
        UserProfile profile = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("404","존재하지 않는 유저"))
                .getUserProfile();

        return profile.getGender().equals(Gender.Male);
    }

    // like 이벤트 처리
    private void processLikeEvent(MapRecord<String,Object,Object> record) {
        Map<Object, Object> value = record.getValue();  // value = Content
        String jsonEvent = (String) value.get("data"); // Content에서 data(Json 문자열) 추출

        // JSON 문자열을 Map으로 변환
        try {
            Map<String, String> likeEvent = objectMapper.readValue(jsonEvent, Map.class);
            Long toUserId = Long.valueOf(likeEvent.get("toUserId"));
            String message = likeEvent.get("message"); // 메시지
            String time = likeEvent.get("time"); // 시간

            // 메시지와 시간 저장
            Map<String, String> notification = new HashMap<>();
            notification.put("message", message);
            notification.put("time", time);
            saveNotification(toUserId, notification);

            // 이벤트 발행
//            sendNotification(toUserId, notification);   // webSocket
            eventPublisher.publishEvent(new NotificationEvent(toUserId,message));   // 이벤트 발행 - 리스너가 탐지

            // 마지막으로 읽은 ID 업데이트
            lastLikeId = record.getId().getValue();
        } catch (Exception e) {
            logger.error("JSON 역 직렬화 실패", e);
        }
    }

    // matching 이벤트 처리
    private void processMatchingEvent(MapRecord<String,Object,Object> record) {
        Map<Object, Object> value = record.getValue();  // value = Content
        String jsonEvent = (String) value.get("data"); // Content에서 data(Json 문자열) 추출

        // JSON 문자열을 Map으로 변환
        try {
            Map<String, String> matchingEvent = objectMapper.readValue(jsonEvent, Map.class);

            Long maleUserId = Long.valueOf(matchingEvent.get("maleUserId"));
            Long femaleUserId = Long.valueOf(matchingEvent.get("femaleUserId"));
            String messageMale = matchingEvent.get("messageMale"); // 남자에게 보낼 메시지
            String messageFemale = matchingEvent.get("messageFemale"); // 여자에게 보낼 메시지
            String time = matchingEvent.get("time"); // 시간

            // 남자 유저에게 보낼 알림
            Map<String, String> notificationMale = new HashMap<>();
            notificationMale.put("message", messageMale);
            notificationMale.put("time", time);
//                            sendNotification(maleUserId, notificationMale);
            eventPublisher.publishEvent(new NotificationEvent(maleUserId,messageMale));
            saveNotification(maleUserId, notificationMale);

            // 여자 유저에게 보낼 알림
            Map<String, String> notificationFemale = new HashMap<>();
            notificationFemale.put("message", messageFemale);
            notificationFemale.put("time", time);
//                            sendNotification(femaleUserId, notificationFemale);
            eventPublisher.publishEvent(new NotificationEvent(femaleUserId,messageFemale));
            saveNotification(femaleUserId, notificationFemale);

            // 마지막으로 읽은 ID 업데이트
            lastMatchId = record.getId().getValue();
        } catch (Exception e) {
            logger.error("JSON 역 직렬화 실패", e);
        }
    }

}