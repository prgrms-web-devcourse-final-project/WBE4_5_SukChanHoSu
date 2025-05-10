package com.NBE4_5_SukChanHoSu.BE.domain.likes.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.Matching;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {
    // 남자 유저 매칭 정보 탐색
    List<Matching> findByMaleUser(UserProfile maleUser);
    // 여자 유저 매칭 정보 탐색
    List<Matching> findByFemaleUser(UserProfile femaleUser);
    // 매칭 정보 존재하는지 확인
    boolean existsByMaleUserAndFemaleUser(UserProfile maleUser, UserProfile femaleUser);

    void deleteByMaleUserAndFemaleUser(UserProfile maleUser, UserProfile femaleUser);
}
