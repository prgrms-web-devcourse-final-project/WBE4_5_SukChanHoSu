package com.NBE4_5_SukChanHoSu.BE.domain.movie.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.dto.response.MovieRankingResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.dto.response.MovieResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.service.MovieService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.Ut;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import com.NBE4_5_SukChanHoSu.BE.global.util.DateUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movie")
@Tag(name = "영화 데이터", description = "영화 Open API")
public class MovieController {

    private final MovieService movieService;
    private final Ut ut;

    @Operation(summary = "박스오피스 조회", description = "1주일전 박스 오피스 탑텐 조회")
    @GetMapping("/weekly")
    public RsData<List<MovieRankingResponse>> searchWeeklyBoxOffice(
            @RequestParam(required = false) String targetDt,    // 필수 파라미터 x
            @RequestParam(defaultValue = "0") String weekGb,
            @RequestParam(defaultValue = "10") String itemPerPage) {

        if(targetDt == null || targetDt.isEmpty()) {
            targetDt = DateUtils.getOneWeekAgoDate();   // 1주일전
        }
        List<MovieRankingResponse> responses = movieService.searchWeeklyBoxOffice(targetDt, weekGb, itemPerPage);
        return new RsData<>("200","박스 오피스 Top 10(일자: "+targetDt+")", responses);
    }

    // 영화 상세 정보 조회
    @Operation(summary = "영화 상세 정보 조회", description = "영화 넘버를 이용하여 상세 정보 조회")
    @GetMapping("/detail")
    public RsData<MovieResponse> getMovieDetail(@RequestParam String movieCd) {
        MovieResponse response = movieService.getMovieDetail(movieCd);
        return new RsData<>("200","영화 상세 정보", response);
    }

    // 보고 싶은 영화 등록
    @Operation(summary = "보고 싶은 영화 등록", description = "레디스에 저장된 영화 넘버를 이용하여 보고 싶은 영화 등록")
    @PostMapping("/bookmark")
    public RsData<MovieResponse> bookmarkMovie(@RequestParam String movieCd) {
        Long profileId = ut.getUserProfileByContextHolder().getUserId();
        String cachedData = movieService.bookmarkMovie(profileId,movieCd);

        // 영화 상세 정보
        MovieResponse response = movieService.getMovieDetail(cachedData);
        return new RsData<>("200","보고 싶은 영화 등록", response);
    }
}