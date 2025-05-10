package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.request;

import lombok.Data;

@Data
public class ReviewRequestDto {
    // todo 영화 객체로 변경 예정 (영화 id로)
    private String title;
    private String content;
    private Double rating;
}
