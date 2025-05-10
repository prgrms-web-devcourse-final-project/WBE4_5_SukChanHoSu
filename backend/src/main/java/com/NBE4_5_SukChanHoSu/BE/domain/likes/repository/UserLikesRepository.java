package com.NBE4_5_SukChanHoSu.BE.domain.likes.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.UserLikes;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserLikesRepository extends JpaRepository<UserLikes, Long> {

    // 반대쪽 관계도 존재하는지 확인
    boolean existsByFromUserAndToUser(UserProfile fromUser, UserProfile toUser);

    void deleteByFromUserAndToUser(UserProfile toUser, UserProfile fromUser);

    // 특정 사용자가 마지막으로 좋아요를 보낸 시간 조회
    @Query("SELECT MAX(ul.likeTime) FROM UserLikes ul WHERE ul.fromUser.userId = :userId")
    LocalDateTime findLastLikeTimeByUserId(@Param("userId") Long userId);
}
