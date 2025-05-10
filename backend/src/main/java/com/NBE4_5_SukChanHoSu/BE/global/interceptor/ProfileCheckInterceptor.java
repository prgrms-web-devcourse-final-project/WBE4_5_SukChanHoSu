package com.NBE4_5_SukChanHoSu.BE.global.interceptor;

import com.NBE4_5_SukChanHoSu.BE.domain.recommend.service.RecommendService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserProfileService;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCheckInterceptor implements HandlerInterceptor {

    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper; // JSON 변환을 위한 ObjectMapper 주입
    private final RecommendService matchingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 프로필 등록은 허용
        if ((uri.equals("/api/profile") && method.equalsIgnoreCase("POST")) ||
                (uri.equals("/api/email/send") && method.equalsIgnoreCase("POST")) ||
                (uri.equals("/api/email/verify") && method.equalsIgnoreCase("POST")) ||
                (uri.equals("/api/token/reissue") && method.equalsIgnoreCase("POST"))
        ) {
            return true;
        }
        Long userId = SecurityUtil.getCurrentUserId();
        boolean hasProfile = userProfileService.existsProfileByUserId(userId);

        if (!hasProfile) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"); // ★ 문자 인코딩 명시
            response.setCharacterEncoding("UTF-8"); // ★ setCharacterEncoding 추가

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "프로필을 먼저 등록해야 합니다.");

            try {
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } catch (IOException e) {
                // 응답 작성 실패 처리 (로깅 등)
                e.printStackTrace();
            }
            return false;
        }

        return true;
    }
}