package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("select count(r), coalesce(avg(r.rating), 0.0) from Review r where r.title = :movieTitle")
    List<Object[]> getReviewStatsByTitle(@Param("movieTitle") String movieTitle);

    // todo 영화 객체의 id 받아서 반환하도록 수정해야댐
    List<Review> findByTitleOrderByCreatedDateDesc(String movieTitle);
}
