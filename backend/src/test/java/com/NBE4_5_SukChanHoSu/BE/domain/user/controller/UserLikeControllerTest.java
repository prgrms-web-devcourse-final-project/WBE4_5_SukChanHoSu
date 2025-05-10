package com.NBE4_5_SukChanHoSu.BE.domain.user.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Gender;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.UserLikeService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.config.BaseTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BaseTestConfig
public class UserLikeControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserLikeService userLikeService;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserService userService;

    private ObjectMapper objectMapper;
    private static String accessToken;
    private static String refreshToken;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        login();
    }

    @AfterAll
    void tearDown() {
        clearRedisData(); // 레디스 데이터 초기화
    }

    private void clearRedisData() {
        Set<String> keys = redisTemplate.keys("user:*"); // "user:*" 패턴의 모든 키 조회
        redisTemplate.delete(keys); // 모든 키 삭제
        Set<String> keys2 = redisTemplate.keys("likes:*"); // "likes:*" 패턴의 모든 키 조회
        redisTemplate.delete(keys2); // 모든 키 삭제
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
        this.accessToken = tokenDto.getAccessToken();
        this.refreshToken = tokenDto.getRefreshToken();
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
        this.accessToken = tokenDto.getAccessToken();
        this.refreshToken = tokenDto.getRefreshToken();
    }


    @DisplayName("like 셋업")
    void setUpLike(Long to) throws Exception {
        mvc.perform(post("/api/users/like")
                        .param("toUserId",to.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
    }

    @Test
    @DisplayName("다른 사용자에게 like 요청")
    void likeUser() throws Exception {
        mvc.perform(post("/api/users/like")
                        .param("toUserId","2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("에게 좋아요를 보냈습니다")));
    }

    @Test
    @DisplayName("like 전송 후 레디스에 저장 확인")
    void createRedisData() throws Exception {
        // Given
        setUpLike(2L);
        Long profileId = 1L;
        // When
        String key = "likes:" + profileId+":"+2L;   // likes:1:2
        String key2 = "user:"+profileId;            // user:1
        Object redisValue = redisTemplate.opsForValue().get(key);
        Object redisValue2 = redisTemplate.opsForValue().get(key2);
        // TTL 조회
        Long ttl = redisTemplate.getExpire(key2, TimeUnit.SECONDS);

        // Then
        assertNotNull(redisValue);
        assertNotNull(redisValue2);
        assertTrue(ttl > 604700);   // 10초 오차 감안해서 갱신된 TTL인지 확인
    }

    @Test
    @DisplayName("사용자의 like 목록 가져오기")
    void getUserLikes() throws Exception {
        //given
        setUpLike(2L);

        //when
        ResultActions action = mvc.perform(get("/api/users/like") // TempUser1의 like 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        // then
        action.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("좋아요한 유저 목록 반환")))
                .andExpect(jsonPath("$.data.userLikes[0].userId").value(2));// user2이 목록에 존재
    }

    @Test
    @DisplayName("사용자의 liked 목록 가져오기")
    void getUserLiked() throws Exception {
        //given
        setUpLike(2L);
        System.out.println("accessToken = " + accessToken);

        login2();

        // when
        ResultActions action = mvc.perform(get("/api/users/liked") // TempUser2를 좋아요한 유저 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action.andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("TempUser2를 좋아요한 유저 목록 반환")))
                .andExpect(jsonPath("$.data.userLikes[0].userId").value(1));// user1이 목록에 존재
    }

    @Test
    @DisplayName("매칭된 상태에서는 사용자의 liked 목록에 안나오는걸 확인")
    void getUserLikedMatchingStatus() throws Exception {
        //given
        setUpLike(2L);

        login2();
        setUpLike(1L);

        // when
        ResultActions getMatching = mvc.perform(get("/api/users/matching") // 매칭 목록 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        ResultActions getLiked = mvc.perform(get("/api/users/liked") // TempUser2를 좋아요한 유저 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        // 매칭 목록에서 조회 가능
        getMatching
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("매칭된 사용자 목록 조회")))
                .andExpect(jsonPath("$.data[*].user.userId").value(1));
        // liked 목록에서는 조회 불가능
        getLiked
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("나를 like 하는 사용자가 없습니다."));
    }

    @Test
    @DisplayName("매칭된 상태에서는 사용자의 likes 목록에 안나오는걸 확인")
    void getUserLikesMatchingStatus() throws Exception {
        //given
        setUpLike(2L);

        login2();
        setUpLike(1L);

        // when
        ResultActions getMatching = mvc.perform(get("/api/users/matching") // 매칭 목록 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        ResultActions getLikes = mvc.perform(get("/api/users/like") // TempUser2를 좋아요한 유저 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        // 매칭 목록에서 조회 가능
        getMatching
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("매칭된 사용자 목록 조회")))
                .andExpect(jsonPath("$.data[*].user.userId").value(1));

        // likes 목록에서는 조회 불가능
        getLikes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("like 한 사용자가 없습니다."));
    }


    @Test
    @DisplayName("사용자의 matching 목록 가져오기")
    void getUserMatch() throws Exception {
        //given
        setUpLike(2L);

        login2();
        // user2 -> user1 맞팔해서 매칭
        ResultActions action = mvc.perform(post("/api/users/like")
                        .param("toUserId","1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        // 매칭 응답 검증
        action.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("매칭 되었습니다.")))
                .andExpect(jsonPath("$.data").exists());
        //when
        ResultActions action2 = mvc.perform(get("/api/users/matching") // TempUser1의 매칭 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("매칭된 사용자 목록 조회")))
                .andExpect(jsonPath("$.data[*].user.userId").value(1));
    }

    @Test
    @DisplayName("중복 like 방지")
    void aVoidDuplicationLike() throws Exception {
        // given
        setUpLike(2L);

        // when
        ResultActions action = mvc.perform(post("/api/users/like")
                        .param("toUserId","2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message",containsString("이미 like 상태입니다.")));
    }

    @Test
    @DisplayName("매칭된 상태에서 like 방지")
    void aVoidDuplicationLike2() throws Exception {
        // given
        setUpLike(2L);

        login2();
        setUpLike(1L);



        // when
        ResultActions action = mvc.perform(post("/api/users/like")
                        .param("toUserId","1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message",containsString("이미 매칭된 상태입니다.")));
    }

    @Test
    @DisplayName("like 취소 - like 상태")
    void cancelLikeUser() throws Exception {
        // given
        setUpLike(2L);

        // when
        ResultActions action = mvc.perform(delete("/api/users/like")
                        .param("toUserId","2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("like 취소")));
    }

    @Test
    @DisplayName("like 취소 - matching 상태")
    void cancelMatchUser() throws Exception {
        // given
        setUpLike(2L); // 11 -> 12
        login2();   // 12 로그인
        setUpLike(1L); // 12 -> 11

        // when
        ResultActions action = mvc.perform(delete("/api/users/like")
                        .param("toUserId","1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("매칭 취소")));
    }

    @Test
    @DisplayName("like 취소 후, 데이터 조회x")
    void cancelMatchGetLikes() throws Exception {
        // given
        setUpLike(2L);

        login2();
        setUpLike(1L);

        mvc.perform(delete("/api/users/like")
                        .param("toUserId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // when
        ResultActions getMatching = mvc.perform(get("/api/users/matching") // TempUser1의 매칭 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        // then
        getMatching
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message", containsString("사용자가 없습니다.")));

        ResultActions getLiked = mvc.perform(get("/api/users/liked") // TempUser2의 Likes 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        getLiked
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message", containsString("사용자가 없습니다.")));
    }

    @Test
    @DisplayName("게이 방지")
    void aVoidGay() throws Exception {
        //given
        long male2 = userProfileRepository.findByGender(Gender.Male).get(1).getUserId();
        long female2 = userProfileRepository.findByGender(Gender.Female).get(1).getUserId();

        // when
        ResultActions maleAction =mvc.perform(post("/api/users/like")
                .param("toUserId",String.valueOf(male2))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));

        login2();
        ResultActions femaleAction =mvc.perform(post("/api/users/like")
                .param("toUserId",String.valueOf(female2))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));
        // then
        maleAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message",containsString("이성간 매칭만 허용")));

        femaleAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message",containsString("이성간 매칭만 허용")));

    }


}