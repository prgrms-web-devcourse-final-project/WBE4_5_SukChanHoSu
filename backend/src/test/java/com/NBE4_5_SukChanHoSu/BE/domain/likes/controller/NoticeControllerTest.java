package com.NBE4_5_SukChanHoSu.BE.domain.likes.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.NoticeService;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.service.MovieService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.config.BaseTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BaseTestConfig
class NoticeControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RestClient restClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private MovieService movieService;
    private static String accessToken;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private SseController sseController;

    private static final String LIKE_STREAM = "like";
    private static final String MATCHING_STREAM = "matching";


    private static boolean started = false; // 한번만 시작하게 제어하는 변수

    // 앱 시작시
    @BeforeAll
    public void init() {
        if (!started) {
            new Thread(noticeService::startLikeStreamListener).start();
            new Thread(noticeService::startMatchStreamListener).start();
            started = true; // 다시 호출되지 않도록
        }
    }

    // 앱 종료시
    @AfterAll
    public void destroy() {
        if (started) {
            noticeService.stop();
            sseController.destroy();
            started = false; // 재사용을 위해 초기화
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        login();
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

    @DisplayName("로그인2")
    void login2() {
        // given
        String email = "initUser2@example.com";
        String rawPassword = "testPassword123!";

        // 로그인
        UserLoginRequest loginDto = new UserLoginRequest();
        loginDto.setEmail(email);
        loginDto.setPassword(rawPassword);

        // when
        LoginResponse tokenDto = userService.login(loginDto);
        accessToken = tokenDto.getAccessToken();
    }

    @DisplayName("like 셋업")
    void setUpLike(Long to) throws Exception {
        mvc.perform(post("/api/users/like")
                        .param("toUserId",to.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
    }

    @AfterAll
    void tearDown() {
        clearRedisData(); // 레디스 데이터 초기화
        ClearStream(LIKE_STREAM);   // 스트림 초기화
        ClearStream(MATCHING_STREAM);
    }

    private void ClearStream(String streamName) {
        // Stream의 모든 레코드 조회
        StreamReadOptions options = StreamReadOptions.empty().count(100); // 한 번에 100개씩 조회
        StreamOffset<String> offset = StreamOffset.create(streamName, ReadOffset.from("0-0")); // 처음부터 조회
        List<MapRecord<String, Object, Object>> records;
        do {
            records = redisTemplate.opsForStream().read(options, offset);
            if (!records.isEmpty()) {
                // 각 레코드 삭제
                for (MapRecord<String, Object, Object> record : records) {
                    redisTemplate.opsForStream().delete(streamName, record.getId().getValue());
                }
            }
        } while (!records.isEmpty()); // 더 이상 레코드가 없을 때까지 반복
    }

    private void clearRedisData() {
        Set<String> keys = redisTemplate.keys("user:*"); // "user:*" 패턴의 모든 키 조회
        redisTemplate.delete(keys); // 모든 키 삭제
        Set<String> keys2 = redisTemplate.keys("likes:*"); // "likes:*" 패턴의 모든 키 조회
        redisTemplate.delete(keys2); // 모든 키 삭제
    }

    @Test
    @DisplayName("알림 목록 조회")
    void getNotifications() throws Exception {
        // Given
        setUpLike(2L);  // 1->2
        login2();

        // When
        ResultActions action = mvc.perform(get("/api/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then
        action.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("알림")))
                .andExpect(jsonPath("$.data[*].timeAgo", everyItem(containsString("전"))))
                .andExpect(jsonPath("$.data[*].message",hasItem(containsString("TempUser1"))));
    }

    @Test
    void markAsRead() throws Exception {
        // Given
        setUpLike(2L);  // 좋아요
        login2();   // 2번 계정으로 로그인
        mvc.perform(get("/api/notice/count")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message",containsString("미확인 알림 갯수")))
            .andExpect(jsonPath("$.data").value(greaterThan(0)));   // 0보다 큼

        // When
        // 읽음 처리
        mvc.perform(post("/api/notice/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then
        mvc.perform(get("/api/notice/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("미확인 알림 갯수")))
                .andExpect(jsonPath("$.data").value(0));   // 0으로 초기화
    }

    @Test
    @DisplayName("미확인 알림 갯수")
    void getUnreadNotificationCount() throws Exception {
        // When
        ResultActions action1 = mvc.perform(get("/api/notice/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        // 응답 파싱
        String responseBody1 = action1.andReturn().getResponse().getContentAsString();
        JSONObject jsonResponse1 = new JSONObject(responseBody1);
        int prevData = jsonResponse1.getInt("data");    // 응답 갯수

        login2();   // 2번 계정으로 로그인 후
        setUpLike(1L);  // 좋아요
        login();    // 다시 1번 계정으로 로그인

        // Then
        mvc.perform(get("/api/notice/count")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("미확인 알림 갯수")))
                .andExpect(jsonPath("$.data").value(prevData+1));   // 이전 값보다 1큼

    }
}