package com.NBE4_5_SukChanHoSu.BE.domain.recommend.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.recommend.entity.RecommendUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RecommendUserRepository extends JpaRepository<RecommendUser, Long> {

    boolean existsByUserIdAndRecommendedUserIdAndType(long userId, Long recommendedUserId, String type);
    void deleteByCreatedAtBefore(LocalDateTime createdAt);
}
