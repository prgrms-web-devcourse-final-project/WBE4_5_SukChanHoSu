package com.NBE4_5_SukChanHoSu.BE.domain.recommend.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.service.MovieService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.Ut;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.recommend.service.RecommendService;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@Tag(name = "Matching API", description = "매칭 관련 API")
public class RecommendController {

    private final RecommendService matchingService;
    private final MovieService movieService;
    private final Ut ut;

    @Operation(summary = "매칭 - 거리로 조회", description = "범위 내에 있는 사용자 무작위 조회")
    @GetMapping("/withinRadius")
    public RsData<UserProfileResponse> getProfileWithinRadius() {
        UserProfileResponse response = matchingService.recommendByDistance();
        return new RsData<>("200", "거리 조회 성공", response);
    }

    @Operation(summary = "매칭 - 태그로 조회", description = "겹치는 태그가 있는 사람중 매칭 조회")
    @GetMapping("/tags")
    public RsData<UserProfileResponse> recommendByTags() {
        UserProfileResponse response = matchingService.recommendUserByTags();
        return new RsData<>("200", "프로필 조회 성공", response);
    }

    @Operation(summary = "매칭 - 보고싶은 영화로 조회", description = "보고싶은 영화가 겹치는 사람들 조회")
    @GetMapping("/movie")
    public RsData<UserProfileResponse> recommendByMovie() {
        UserProfile profile = ut.getUserProfileByContextHolder();

        String key =  "user:" + profile.getUserId();
        String movieCd = movieService.getBookmarkDataFromRedis(key);

        UserProfileResponse response = matchingService.recommendUserByMovie(profile,movieCd);

        return new RsData<>("200", "프로필 조회 성공", response);
    }
}