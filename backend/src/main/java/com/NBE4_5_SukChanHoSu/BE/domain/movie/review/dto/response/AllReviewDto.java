package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AllReviewDto {
    private List<ReviewResponseDto> reviews;
    private Long totalReviews;
    private Double totalRating;

    public AllReviewDto(List<ReviewResponseDto> reviewList, Long totalReviews, Double totalRating) {
        this.reviews = reviewList;
        this.totalReviews = totalReviews;
        this.totalRating = Math.round(totalRating * 10) / 10.0;
    }
}
