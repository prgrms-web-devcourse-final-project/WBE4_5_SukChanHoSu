package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.request.ReviewRequestDto;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.response.AllReviewDto;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.response.ReviewResponseDto;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.service.ReviewService;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movie/review")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public RsData<ReviewResponseDto> createReviewPost(@RequestBody ReviewRequestDto requestDto) {
        return new RsData<>(
                "200",
                "성공",
                reviewService.createReviewPost(requestDto)
        );
    }

    // 단건 조회
    @GetMapping("/{reviewId}")
    public RsData<ReviewResponseDto> getReviewById(@PathVariable Long reviewId) {
        return new RsData<>(
                "200",
                "성공",
                reviewService.getOneReview(reviewId)
        );
    }

    // 다건 조회
    // todo 영화 id로 변경해야댐
    @GetMapping
    public RsData<AllReviewDto> getAllReviews(
            @RequestParam String movieTitle,
            @RequestParam(required = false) String sort) {
        return new RsData<>(
                "200",
                "성공",
                reviewService.getAllReviewsByTitle(movieTitle, sort)
        );
    }

    @PatchMapping("/{reviewId}")
    public RsData<?> updateReview(@PathVariable Long reviewId, @RequestBody ReviewRequestDto requestDto) {
        reviewService.updateReview(reviewId, requestDto);
        return new RsData<>(
                "200",
                "성공"
        );
    }

    @DeleteMapping("/{reviewId}")
    public RsData<?> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return new RsData<>(
                "200",
                "성공"
        );
    }
}
