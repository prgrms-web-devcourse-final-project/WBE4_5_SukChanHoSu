package com.NBE4_5_SukChanHoSu.BE.domain.likes.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.NotificationEvent;
import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.StreamManager;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.config.BaseTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BaseTestConfig
class SseControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private SseController sseController;
    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory; // Redis 연결 관리

    private static String accessToken;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        login();
        connectRedis();
    }

    @DisplayName("Redis 연결")
    public void connectRedis(){
        if (!lettuceConnectionFactory.isRunning()) {
            lettuceConnectionFactory.start(); // 재시작
        }
    }

    @DisplayName("로그인")
    void login() {
        // given
        String email = "initUser1@example.com";
        String rawPassword = "testPassword123!";

        // 로그인
        UserLoginRequest loginDto = new UserLoginRequest();
        loginDto.setEmail(email);
        loginDto.setPassword(rawPassword);

        // when
        LoginResponse tokenDto = userService.login(loginDto);
        accessToken = tokenDto.getAccessToken();
    }

    @Test
    @DisplayName("SSE 연결")
    void createConnection() throws Exception {
        // Given
        Long userId = 1L;

        // When
        mvc.perform(get("/api/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
//                .andExpect(jsonPath("$.code").value("200"))
//                .andExpect(jsonPath("$.message",containsString("미확인 알림 갯수")))
//                .andExpect(jsonPath("$.data").value(prevData+1));   // 이전 값보다 1큼

        // Then
        SseEmitter emitter = sseController.getEmitters().get(userId);
        // 연결 생성 검증
        assertNotNull(emitter);
    }

    @Test
    @DisplayName("알림 전송")
    void sendNotification() throws Exception {
        // Given
        Long userId = 1L;

        // When
        mvc.perform(get("/api/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        // 이벤트 발행
        NotificationEvent event = new NotificationEvent(userId, "Test Message");
        sseController.handleNotification(event);

        // Then
        // 전송한 ID가 포함되어있는지 검증
        assertTrue(sseController.getEmitters().containsKey(userId));

    }

    @Test
    @DisplayName("SSE 연결 종료")
    void destroy() throws Exception {
        // Given
        Long userId = 1L;
        // SSE 연결 생성
        mvc.perform(get("/api/sse")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andDo(print())
                .andExpect(status().isOk());
        // 이벤트 발행
        NotificationEvent event = new NotificationEvent(userId, "Test Message");
        sseController.handleNotification(event);

        // When
        sseController.destroy();

        // Then
        assertFalse(sseController.getEmitters().containsKey(userId));
    }

    @Test
    @DisplayName("매칭 알림 테스트")
    void testLikeAndNotification() throws Exception {
        // 1. 홀수 유저(1, 3, 5, 7, 9)가 4번 유저에게 like를 보냄
        int[] oddUserIds = {1, 3, 5, 7, 9};
        for (int userId : oddUserIds) {
            // 홀수 유저 로그인
            UserLoginRequest loginDto = new UserLoginRequest();
            loginDto.setEmail("initUser" + userId + "@example.com");
            loginDto.setPassword("testPassword123!");
            LoginResponse tokenDto = userService.login(loginDto);
            String oddUserAccessToken = tokenDto.getAccessToken();

            // 4번 유저에게 like 보내기
            mvc.perform(post("/api/users/like")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + oddUserAccessToken)
                            .param("toUserId", "4"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // 2. 4번 유저가 홀수 유저(1, 3, 5, 7, 9)에게 like를 보냄
        // 4번 유저 로그인
        UserLoginRequest loginDto = new UserLoginRequest();
        loginDto.setEmail("initUser4@example.com");
        loginDto.setPassword("testPassword123!");
        LoginResponse tokenDto = userService.login(loginDto);
        String user4AccessToken = tokenDto.getAccessToken();

        for (int userId : oddUserIds) {
            // 홀수 유저에게 like 보내기
            mvc.perform(post("/api/users/like")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + user4AccessToken)
                            .param("toUserId", String.valueOf(userId)))
                    .andDo(print())
                    .andExpect(status().isOk());

            // 약간의 시간차 추가 (1초)
            TimeUnit.SECONDS.sleep(1);
        }

        // 3. SSE 알림 확인
        // 4번 유저의 SSE 연결 생성
        mvc.perform(get("/api/sse")
                        .header("Authorization", "Bearer " + user4AccessToken)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andDo(print())
                .andExpect(status().isOk());

        // 홀수 유저의 SSE 연결 생성
        for (int userId : oddUserIds) {
            // 홀수 유저 로그인
            UserLoginRequest oddUserLoginDto = new UserLoginRequest();
            oddUserLoginDto.setEmail("initUser" + userId + "@example.com");
            oddUserLoginDto.setPassword("testPassword123!");
            LoginResponse oddUserTokenDto = userService.login(oddUserLoginDto);
            String oddUserAccessToken = oddUserTokenDto.getAccessToken();

            // SSE 연결 생성
            mvc.perform(get("/api/sse")
                            .header("Authorization", "Bearer " + oddUserAccessToken)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // 4. 알림 전송 확인
        // 4번 유저에게 온 알림 확인
        assertTrue(sseController.getEmitters().containsKey(4L));

        // 홀수 유저에게 온 알림 확인
        for (int userId : oddUserIds) {
            assertTrue(sseController.getEmitters().containsKey((long) userId));
        }
    }
}