package com.NBE4_5_SukChanHoSu.BE.domain.user.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.recommend.service.RecommendService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserProfileService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.config.BaseTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BaseTestConfig
class UserProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private RecommendService matchingService;

    @Autowired
    private ObjectMapper objectMapper;

//    // 회원 가입 + 로그인 후 JWT 토큰 얻기
//    private String createToken(String email) {
//        memberService.join(new MemberSignUpRequestDto(email, "1234", "1234"));
//        JwtTokenDto jwt = memberService.login(new MemberLoginRequestDto(email, "1234"));
//        return jwt.getAccessToken();
//    }
//
//    @Test
//    void 내_프로필_조회_성공() throws Exception {
//        String token = createToken("user1@example.com");
//
//        // 우회: SecurityContext 강제 설정
//        Member member = memberRepository.findByEmail("user1@example.com").orElseThrow();
//        CustomUserDetails userDetails = new CustomUserDetails(member);
//        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(auth);
//
//        mockMvc.perform(get("/api/profile/me")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("200"));
//    }
//
//    @Test
//    void 프로필_등록_성공() throws Exception {
//        String token = createToken("user2@example.com");
//
//        ProfileRequestDto dto = ProfileRequestDto.builder()
//                .nickname("testnick")
//                .gender(Gender.FEMALE)
//                .introduce("자기소개입니다.")
//                .build();
//        String body = objectMapper.writeValueAsString(dto);
//
//        mockMvc.perform(post("/api/profile")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.code").value("201"));
//    }

}
