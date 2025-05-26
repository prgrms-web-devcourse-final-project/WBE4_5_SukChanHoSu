package com.NBE4_5_SukChanHoSu.BE.domain.user.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.recommend.service.RecommendService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.Ut;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.NicknameCheckResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.ProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserProfileService;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "프로필 관리", description = "프로필 수정 및 닉네임 중복 검사 등 API")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final Ut ut;

    @Operation(summary = "프로필 등록 (DTO)", description = "회원가입 후 최초 프로필 정보 등록")
    @PostMapping(value = "/info")
    @ResponseStatus(HttpStatus.CREATED)
    public RsData<ProfileResponse> createProfileInfo(@RequestBody @Valid ProfileRequest dto) throws IOException {
        ProfileResponse response = userProfileService.createProfile(SecurityUtil.getCurrentUserId(), dto); // 이미지 리스트는 null로 전달
        return new RsData<>("201", "프로필 정보 등록 완료", response);
    }

    @Operation(summary = "프로필 이미지 등록", description = "프로필 이미지 업로드 (선택 사항, 기존 프로필 필요)")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<ProfileResponse> uploadProfileImages(@RequestPart(value = "profileImages") List<MultipartFile> profileImages) throws IOException {
        // UserProfileService의 해당 메서드를 수정하여 이미지 업로드만 처리하도록 하거나,
        // 기존 createProfile 메서드를 호출하여 처리할 수 있습니다.
        ProfileResponse response = userProfileService.addProfileImages(SecurityUtil.getCurrentUserId(), profileImages); // 예시 메서드
        return new RsData<>("200", "프로필 이미지 등록 완료", response);
    }

    @Operation(summary = "프로필 정보 수정", description = "닉네임, 성별, 위치 등 프로필 정보 수정")
    @PutMapping(value = "/info", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RsData<ProfileResponse> updateProfileInfo(@RequestBody @Valid ProfileUpdateRequest dto) throws IOException {
        ProfileResponse response = userProfileService.updateProfile(SecurityUtil.getCurrentUserId(), dto, null, null);
        return new RsData<>("200", "프로필 정보 수정 완료", response);
    }

    @Operation(summary = "프로필 이미지 수정", description = "프로필 이미지 추가 및 삭제")
    @PutMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<ProfileResponse> updateProfileImages(
            @RequestPart(value = "profileImages", required = false) List<MultipartFile> profileImages) throws IOException {
        ProfileResponse response = userProfileService.updateProfileImages(SecurityUtil.getCurrentUserId(), profileImages);
        return new RsData<>("200", "프로필 이미지 수정 완료", response);
    }

    @Operation(summary = "프로필 이미지 삭제", description = "특정 프로필 이미지 URL 목록을 삭제합니다.")
    @PutMapping("/images/delete")
    @ResponseStatus(HttpStatus.OK)
    public RsData<ProfileResponse> deleteProfileImages(@RequestBody List<String> imagesToDelete) {
        ProfileResponse response = userProfileService.deleteProfileImages(SecurityUtil.getCurrentUserId(), imagesToDelete);
        return new RsData<>("200", "프로필 이미지 삭제 완료", response); // ProfileResponse를 포함한 RsData 반환
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
        UserProfile profile = ut.getUserProfileByContextHolder();
        return new RsData<>("200", "프로필 조회 성공", profile.getUser());
    }

    @Operation(summary = "내 프로필 조회", description = "자신의 프로필 정보 조회")
    @GetMapping("/profile/me")
    //todo 임시, 이후 삭제
    public RsData<UserProfile> getMyProfile1() {
        UserProfile profile = ut.getUserProfileByContextHolder();
        return new RsData<>("200", "프로필 조회 성공", profile);
    }

    @Operation(summary = "범위 조절", description = "탐색 범위 조절")
    @PutMapping("/radius")
    public RsData<ProfileResponse> setRadius(@RequestParam Integer radius) {
        UserProfile profile = ut.getUserProfileByContextHolder();

        int updatedRadius = userProfileService.setRadius(profile, radius);
        ProfileResponse response = new ProfileResponse(profile);

        return new RsData<>("200", "수정된 범위: "+ updatedRadius, response);
    }

}