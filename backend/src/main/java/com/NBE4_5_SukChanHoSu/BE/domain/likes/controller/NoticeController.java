package com.NBE4_5_SukChanHoSu.BE.domain.likes.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.NoticeService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.Ut;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "Event 관련 API")
public class NoticeController {

    private final NoticeService noticeService;
    private final Ut ut;

    // 알림 목록 조회
    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "읽지 않은 알림 목록 조회")
    public RsData<List<Map<String, String>>>  getNotifications(){
        Long userId = ut.getUserProfileByContextHolder().getUserId();
        List<Map<String, String>> responses = noticeService.getNotifications(userId);
        return new RsData<>("200","알림 목록 조회", responses);
    }

    // 알림 확인 처리
    @PostMapping("/read")
    @Operation(summary = "알림 읽음 처리", description = "읽은 알림 삭제")
    public RsData<String> markAsRead() {
        Long userId = ut.getUserProfileByContextHolder().getUserId();
        noticeService.markAsRead(userId);
        return new RsData<>("200","완료");
    }

    // 미확인 알림 개수 조회
    @GetMapping("/count")
    @Operation(summary = "알림 갯수 조회", description = "읽지 않은 알림 갯수 조회")
    public RsData<Integer> getUnreadNotificationCount() {
        Long userId = ut.getUserProfileByContextHolder().getUserId();
        int count = noticeService.getUnreadNotificationCount(userId);
        return new RsData<>("200","미확인 알림 갯수", count);
    }
}
