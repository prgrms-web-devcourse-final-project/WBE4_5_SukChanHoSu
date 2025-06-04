package com.NBE4_5_SukChanHoSu.BE.domain.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/health")
@Tag(name = "부하 테스트", description = "테스트를 위한 인증없는 페이지")
public class healthController {

    @GetMapping
    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 동작 중인지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "서버가 정상적으로 동작 중입니다.")
    public String healthCheck() {
        return "Server is running!";
    }

    @GetMapping("/time")
    @Operation(summary = "서버 시간 확인", description = "서버의 현재 시간을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "서버 시간이 반환되었습니다.")
    public String getServerTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Current server time: " + now.format(formatter);
    }

    @GetMapping("/status")
    @Operation(summary = "서버 상태 JSON 확인", description = "서버 상태를 JSON 형식으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "서버 상태가 반환되었습니다.")
    public Map<String, String> getServerStatus() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Server is running!");
        return response;
    }

}