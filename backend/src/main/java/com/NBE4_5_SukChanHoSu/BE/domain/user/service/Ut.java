package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.UserNotFoundException;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class Ut {
    private final UserProfileRepository userProfileRepository;

    public UserProfile getUserProfileByContextHolder(){
        UserProfile profile = SecurityUtil.getCurrentUser().getUserProfile();
        Long userId = profile.getUserId();
        return findUser(userId);
    }

    public UserProfile findUser(Long userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("401", "존재하지 않는 유저입니다."));
    }
}
