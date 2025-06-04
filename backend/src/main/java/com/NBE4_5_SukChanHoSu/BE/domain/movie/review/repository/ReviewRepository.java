package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.entity.Movie;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("select count(r), coalesce(avg(r.rating), 0.0) from Review r join r.movie m where m.movieId = :movieId")
    List<Object[]> getReviewStatsByMovie(@Param("movieId") Long movieId);

    @Query("SELECT r FROM Review r JOIN FETCH r.movie WHERE r.id = :id")
    Optional<Review> findByIdWithMovie(@Param("id") Long id);

    List<Review> findByMovie_MovieIdOrderByCreatedDateDesc(Long movieId);
    List<Review> findByMovie_MovieIdOrderByLikeCountDescCreatedDateDesc(Long movieId);

    List<Review> findAllByMovie_MovieId(Long movieId);

    @Query("SELECT DISTINCT r.movie FROM Review r")
    List<Movie> findDistinctMovies();

    @Query("SELECT r FROM Review r JOIN FETCH r.movie")
    List<Review> findAllWithMovie();
}
