package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.UserLikesRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.ProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Genre;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.NoRecommendException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.UserNotFoundException;
import com.NBE4_5_SukChanHoSu.BE.global.util.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserLikesRepository userLikesRepository;
    private final S3Util s3Util;
    // 사용자별 추천 리스트 관리
    private Map<Long, List<UserProfile>> recommendedUsersMap = new HashMap<>();

    @Transactional
    public ProfileResponse createProfile(Long userId, ProfileRequest dto, MultipartFile profileImageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다."));

        // 해당 userId를 가진 UserProfile이 이미 존재하는지 확인
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);
        String profileImageUrl = null;
        UserProfile userProfile;
        profileImageUrl = s3Util.uploadFile(profileImageFile);

        // 프로필이 없으면 새로 생성
        if (!existingProfile.isPresent()) {
            userProfile = new UserProfile();
            userProfile = UserProfile.builder()
                    .user(user)
                    .nickName(dto.getNickname())
                    .gender(dto.getGender())
                    .profileImage(profileImageUrl)
                    .latitude(dto.getLatitude())
                    .longitude(dto.getLongitude())
                    .birthdate(dto.getBirthdate())
                    .introduce(dto.getIntroduce())
                    .searchRadius(dto.getSearchRadius())
                    .lifeMovie(dto.getLifeMovie())
                    .favoriteGenres(dto.getFavoriteGenres())
                    .watchedMovies(dto.getWatchedMovies())
                    .preferredTheaters(dto.getPreferredTheaters())
                    .build();

        } else {
            // 프로필이 이미 존재하면 예외 발생 (또는 업데이트 로직 처리 - API 역할에 따라 다름)
            throw new IllegalStateException("이미 프로필이 등록된 사용자입니다.");
        }

        UserProfile savedUserProfile = userProfileRepository.save(userProfile);
        return new ProfileResponse(savedUserProfile);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest dto, MultipartFile profileImageFile) throws IOException {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String profileImageUrl = userProfile.getProfileImage();

        // 프로필 이미지가 새로 업로드된 경우에만 S3 저장
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            profileImageUrl = s3Util.uploadFile(profileImageFile);
        }

        userProfile = UserProfile.builder()
                .user(userProfile.getUser()) // 기존 User 연결 유지
                .userId(userProfile.getUserId()) // 기존 ID 유지
                .nickName(dto.getNickname())
                .gender(dto.getGender())
                .profileImage(profileImageUrl)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .birthdate(dto.getBirthdate())
                .introduce(dto.getIntroduce())
                .searchRadius(dto.getSearchRadius())
                .lifeMovie(dto.getLifeMovie())
                .favoriteGenres(dto.getFavoriteGenres())
                .watchedMovies(dto.getWatchedMovies())
                .preferredTheaters(dto.getPreferredTheaters())
                .build();
        UserProfile savedUserProfile = userProfileRepository.save(userProfile);
        return new ProfileResponse(savedUserProfile);
    }

    public boolean isNicknameDuplicated(String nickname) {
        return userProfileRepository.existsByNickName(nickname);
    }

    public ProfileResponse getMyProfile(Long userId) {
        UserProfile userProfile = userProfileRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return new ProfileResponse(userProfile);
    }

    public boolean existsProfileByUserId(Long userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    @Transactional
    public void setRadius(UserProfile userProfile, Integer radius) {
        userProfile.setSearchRadius(radius);
    }
}
