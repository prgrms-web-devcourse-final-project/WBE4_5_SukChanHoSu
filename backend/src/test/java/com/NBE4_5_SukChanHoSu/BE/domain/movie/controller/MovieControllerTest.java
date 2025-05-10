package com.NBE4_5_SukChanHoSu.BE.domain.movie.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.service.MovieService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.config.BaseTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BaseTestConfig
class MovieControllerTest {
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
    private static String refreshToken;

    private static final String BOX_OFFICE_KEY = "weeklyBoxOffice";
    private static final String MOVIE_KEY = "MovieCd:";
    private static final String movieCd = "20232394";

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
        this.accessToken = tokenDto.getAccessToken();
        this.refreshToken = tokenDto.getRefreshToken();
    }

    @Test
    @DisplayName("주간 박스오피스 조회")
    void searchWeeklyBoxOffice() throws Exception {
        // when
        ResultActions action = mvc.perform(get("/api/movie/weekly") // 박스 오피스 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        action.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("박스 오피스 Top 10")))
                .andExpect(jsonPath("$.data.size()").value(10));
        assertTrue(redisTemplate.hasKey(BOX_OFFICE_KEY));
    }

    @Test
    @DisplayName("주간 박스오피스 조회 - 캐싱 검증")
    void cachingWeeklyBoxOffice() throws Exception {
        // Given
        redisTemplate.delete("weeklyBoxOffice");
        long startTime1 = System.currentTimeMillis();
        ResultActions action1 = mvc.perform(get("/api/movie/weekly") // 박스 오피스 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        long endTime1 = System.currentTimeMillis();
        long responseTime1 = endTime1 - startTime1; // 응답 시간

        String responseBody1 = action1.andReturn().getResponse().getContentAsString();
        JSONObject jsonResponse1 = new JSONObject(responseBody1);
        JSONArray dataArray1 = jsonResponse1.getJSONArray("data");

        action1.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("박스 오피스 Top 10")))
                .andExpect(jsonPath("$.data.size()").value(10));
        assertTrue(redisTemplate.hasKey(BOX_OFFICE_KEY));

        // When
        long startTime2 = System.currentTimeMillis();
        ResultActions action2 = mvc.perform(get("/api/movie/weekly") // 박스 오피스 데이터 가져오기
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());
        long endTime2 = System.currentTimeMillis();
        long responseTime2 = endTime2 - startTime2;

        String responseBody2 = action2.andReturn().getResponse().getContentAsString();
        JSONObject jsonResponse2 = new JSONObject(responseBody2);
        JSONArray dataArray2 = jsonResponse2.getJSONArray("data");

        // Then
        action2.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("박스 오피스 Top 10")))
                .andExpect(jsonPath("$.data.size()").value(10));
        // 두 데이터 배열의 크기가 같은지 확인
        assertEquals(dataArray1.length(), dataArray2.length());

        // 필드 값 비교
        for (int i = 0; i < dataArray1.length(); i++) {
            JSONObject movie1 = dataArray1.getJSONObject(i);
            JSONObject movie2 = dataArray2.getJSONObject(i);

            // 필드별로 값이 일치하는지 확인
            assertEquals(movie1.getString("movieNm"), movie2.getString("movieNm"));
            assertEquals(movie1.getString("posterUrl"), movie2.getString("posterUrl"));
            assertEquals(movie1.getInt("rank"), movie2.getInt("rank"));
            assertEquals(movie1.getString("audiAcc"), movie2.getString("audiAcc"));
            assertEquals(movie1.getString("movieCd"), movie2.getString("movieCd"));
        }

        // 응답 속도 비교
        assertTrue(responseTime2 < responseTime1);
        System.out.println("responseTime1: " + responseTime1);
        System.out.println("responseTime2: " + responseTime2);
    }

    @Test
    @DisplayName("영화 상세 정보")
    void getMovieDetail() throws Exception {
        // Given
        String key = MOVIE_KEY+movieCd;
        redisTemplate.delete(key);

        long startTime1 = System.currentTimeMillis();
        ResultActions action1 = mvc.perform(get("/api/movie/detail") // 박스 오피스 데이터 가져오기
                        .param("movieCd",movieCd)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        long endTime1 = System.currentTimeMillis();
        long responseTime1 = endTime1 - startTime1; // 응답 시간

        String responseBody1 = action1.andReturn().getResponse().getContentAsString();
        JSONObject jsonResponse1 = new JSONObject(responseBody1);
        JSONObject data1 = jsonResponse1.getJSONObject("data");

        action1.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("영화 상세 정보")));
        // 캐싱 검증
        assertTrue(redisTemplate.hasKey(key));

        // When
        long startTime2 = System.currentTimeMillis();
        ResultActions action2 = mvc.perform(get("/api/movie/detail") // 박스 오피스 데이터 가져오기
                        .param("movieCd",movieCd)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        long endTime2 = System.currentTimeMillis();
        long responseTime2 = endTime2 - startTime2;

        String responseBody2 = action2.andReturn().getResponse().getContentAsString();
        JSONObject jsonResponse2 = new JSONObject(responseBody2);
        JSONObject data2 = jsonResponse2.getJSONObject("data");

        // Then
        action2.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message",containsString("영화 상세 정보")));

        // 데이터 비교
        assertEquals(data1.getString("movieNm"), data2.getString("movieNm"));
        assertEquals(data1.getString("openDt"), data2.getString("openDt"));
        assertEquals(data1.getString("showTm"), data2.getString("showTm"));
        assertEquals(data1.getString("director"), data2.getString("director"));
        assertEquals(data1.getString("posterUrl"), data2.getString("posterUrl"));

        // 응답 속도 비교
        assertTrue(responseTime2 < responseTime1);
        System.out.println("responseTime1: " + responseTime1);
        System.out.println("responseTime2: " + responseTime2);
    }

    @Test
    @DisplayName("보고싶은 영화 등록")
    void bookmarkMovie() throws Exception {
        // Given
        Long profileId = 1L;

        String key = "user:" + profileId;

        // When
        ResultActions action = mvc.perform(post("/api/movie/bookmark")
                        .param("movieCd", movieCd)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then
        action.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("보고 싶은 영화 등록"));

        String cachedData = movieService.getBookmarkDataFromRedis(key);
        assertEquals(cachedData,movieCd);
    }
}