package com.NBE4_5_SukChanHoSu.BE.domain.recommend.service;

import com.NBE4_5_SukChanHoSu.BE.domain.recommend.repository.RecommendUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteSchedular {
    private final RecommendUserRepository recommendUserRepository;

    @Scheduled(cron = "0 0 0 * * ?")    // 매일 자정
    public void cleanUp(){
        LocalDateTime baseline = LocalDateTime.now().minusDays(30);
        recommendUserRepository.deleteByCreatedAtBefore(baseline);  // 30일 이전 데이터 삭제
    }
}
