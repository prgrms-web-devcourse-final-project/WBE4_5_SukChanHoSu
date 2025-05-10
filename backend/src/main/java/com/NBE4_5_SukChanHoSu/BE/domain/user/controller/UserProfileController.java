package com.NBE4_5_SukChanHoSu.BE.domain.user.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserMatchingService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.NicknameCheckResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.ProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "프로필 관리", description = "프로필 수정 및 닉네임 중복 검사 등 API")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserMatchingService matchingService;

    @Operation(summary = "프로필 등록", description = "회원가입 후 최초 프로필 등록")
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public RsData<ProfileResponse> createProfile(@ModelAttribute  ProfileRequest dto,
                                                 @RequestPart(value = "profileImage", required = false) MultipartFile profileImageFile) throws IOException {

        ProfileResponse response = userProfileService.createProfile(SecurityUtil.getCurrentUserId(), dto,profileImageFile);
        return new RsData<>("201", "프로필 등록 완료", response);
    }

    @Operation(summary = "프로필 수정", description = "닉네임, 성별, 위치 등 프로필 정보 수정")
    @PutMapping(consumes = "multipart/form-data")
    public RsData<ProfileResponse> updateProfile(@ModelAttribute ProfileUpdateRequest dto,
                                                 @RequestPart(value = "profileImage", required = false) MultipartFile profileImageFile) throws IOException {
        ProfileResponse response = userProfileService.updateProfile(SecurityUtil.getCurrentUserId(), dto,profileImageFile);
        return new RsData<>("200", "프로필 수정 완료", response);
    }

    @Operation(summary = "내 프로필 조회", description = "자신의 프로필 정보 조회")
    @GetMapping("/me")
    public RsData<ProfileResponse> getMyProfile() {
        ProfileResponse response = userProfileService.getMyProfile(SecurityUtil.getCurrentUserId());
        return new RsData<>("200", "프로필 조회 성공", response);
    }

    @Operation(summary = "닉네임 중복 검사", description = "사용 가능한 닉네임인지 확인합니다.")
    @GetMapping("/check-nickname")
    public RsData<NicknameCheckResponse> checkNickname(@Parameter(name = "nickname", description = "중복 검사할 닉네임") @RequestParam String nickname) {
        boolean duplicated = userProfileService.isNicknameDuplicated(nickname);
        return new RsData<>("200", "닉네임 중복 검사 성공", new NicknameCheckResponse(nickname, duplicated));
    }

    @Operation(summary = "프로필로 유저 객체 조회", description = "프로필 엔티티를 이용하여 유저 정보 가져오는지 확인")
    @GetMapping("/user")
    public RsData<User> getUser() {
        User user = SecurityUtil.getCurrentUser();
        Long profileId = user.getUserProfile().getUserId();
        UserProfile profile = matchingService.findUser(profileId);
        return new RsData<>("200", "프로필 조회 성공", profile.getUser());
    }

    @Operation(summary = "내 프로필 조회", description = "자신의 프로필 정보 조회")
    @GetMapping("/profile/me")
    //todo 임시, 이후 삭제
    public RsData<UserProfile> getMyProfile1() {
        User user = SecurityUtil.getCurrentUser();
        Long profileId = user.getUserProfile().getUserId();
        UserProfile userProfile = matchingService.findUser(profileId);
        return new RsData<>("200", "프로필 조회 성공", userProfile);
    }

    @Operation(summary = "범위 조절", description = "탐색 범위 조절")
    @PutMapping("/radius")
    public RsData<?> setRadius(@RequestParam Integer radius) {
        User user = SecurityUtil.getCurrentUser();
        Long profileId = user.getUserProfile().getUserId();

        UserProfile userProfile = matchingService.findUser(profileId);
        userProfileService.setRadius(userProfile, radius);

        return new RsData<>("200", "프로필 조회 성공", userProfile);
    }

}