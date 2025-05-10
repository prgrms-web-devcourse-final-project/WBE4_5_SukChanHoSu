package com.NBE4_5_SukChanHoSu.BE.domain.recommend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class RecommendUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long recommendedUserId;
    private String type; // 추천 타입 ("tags", "distance")

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public RecommendUser(Long userId, Long recommendedUserId, String type) {
        this.userId = userId;
        this.recommendedUserId = recommendedUserId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public RecommendUser(Long userId, Long recommendedUserId, String type,LocalDateTime createdAt) {
        this.userId = userId;
        this.recommendedUserId = recommendedUserId;
        this.type = type;
        this.createdAt = createdAt;
    }

}