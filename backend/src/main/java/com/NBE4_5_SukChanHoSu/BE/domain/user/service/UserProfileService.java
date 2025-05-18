package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.repository.UserLikesRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.ProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
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
    public ProfileResponse createProfile(Long userId, ProfileRequest dto, List<MultipartFile> profileImageFiles) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다."));

        // 해당 userId를 가진 UserProfile이 이미 존재하는지 확인
        if (userProfileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("이미 프로필이 등록된 사용자입니다.");
        }

        // 프로필 이미지 업로드
        List<String> profileImageUrls = new ArrayList<>();
        if (profileImageFiles != null) {
            for (MultipartFile file : profileImageFiles) {
                String imageUrl = s3Util.uploadFile(file);
                profileImageUrls.add(imageUrl);
            }
        }

        // 프로필이 없으면 새로 생성
        UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .nickName(dto.getNickname())
                    .gender(dto.getGender())
                    .profileImages(profileImageUrls)
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

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest dto, List<MultipartFile> newProfileImages,List<String> imagesToDelete) throws IOException {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        //기존 이미지 삭제
        if (imagesToDelete != null) {
            List<String> currentImages = new ArrayList<>(userProfile.getProfileImages());
            for (String imageUrl : imagesToDelete) {
                if (currentImages.contains(imageUrl)) {
                    currentImages.remove(imageUrl);
                    s3Util.deleteFile(imageUrl);  // S3에서 이미지 삭제
                }
            }
            userProfile.setProfileImages(currentImages);  // 삭제된 이미지 반영
        }

        //새로운 이미지 추가
        if (newProfileImages != null && !newProfileImages.isEmpty()) {
            List<String> currentImages = new ArrayList<>(userProfile.getProfileImages());
            for (MultipartFile file : newProfileImages) {
                String imageUrl = s3Util.uploadFile(file);
                if (!currentImages.contains(imageUrl)) {
                    currentImages.add(imageUrl);
                }
            }
            userProfile.setProfileImages(currentImages);  // 추가된 이미지 반영
        }

        userProfile = UserProfile.builder()
                .user(userProfile.getUser())
                .userId(userProfile.getUserId())
                .nickName(dto.getNickname() != null ? dto.getNickname() : userProfile.getNickName())
                .gender(dto.getGender() != null ? dto.getGender() : userProfile.getGender())
                .profileImages(userProfile.getProfileImages())
                .latitude(dto.getLatitude() != null ? dto.getLatitude() : userProfile.getLatitude())
                .longitude(dto.getLongitude() != null ? dto.getLongitude() : userProfile.getLongitude())
                .birthdate(dto.getBirthdate() != null ? dto.getBirthdate() : userProfile.getBirthdate())
                .introduce(dto.getIntroduce() != null ? dto.getIntroduce() : userProfile.getIntroduce())
                .searchRadius(Optional.ofNullable(dto.getSearchRadius()).orElse(userProfile.getSearchRadius()))
                .lifeMovie(dto.getLifeMovie() != null ? dto.getLifeMovie() : userProfile.getLifeMovie())
                .favoriteGenres(dto.getFavoriteGenres() != null ? dto.getFavoriteGenres() : userProfile.getFavoriteGenres())
                .watchedMovies(dto.getWatchedMovies() != null ? dto.getWatchedMovies() : userProfile.getWatchedMovies())
                .preferredTheaters(dto.getPreferredTheaters() != null ? dto.getPreferredTheaters() : userProfile.getPreferredTheaters())
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
    public int setRadius(UserProfile userProfile, Integer radius) {
        userProfile.setSearchRadius(radius);
        return userProfile.getSearchRadius();
    }
}
