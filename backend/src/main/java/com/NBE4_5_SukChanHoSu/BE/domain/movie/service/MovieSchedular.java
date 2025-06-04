package com.NBE4_5_SukChanHoSu.BE.domain.movie.service;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.entity.Movie;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.repository.MovieRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.entity.Review;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieSchedular {

    @Autowired
    MovieRepository movieRepository;
    @Autowired
    ReviewRepository reviewRepository;

    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void updateReview() {
        System.out.println("스케줄러 실행됨");
        List<Movie> movieList = reviewRepository.findDistinctMovies();
        List<Review> reviews = reviewRepository.findAllWithMovie();

        movieList.forEach(movie -> {
            System.out.println("movie code"+ movie.getMovieId());
            Movie m = movieRepository.findById(movie.getMovieId()).orElseThrow();
            List<Review> reviewsForMovie = reviews.stream()
                    .filter(r -> r.getMovie().getMovieId().equals(m.getMovieId()))
                    .toList();

            Double avgRating = reviewsForMovie.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);

            m.setRating(String.format("%.2f", avgRating));
            System.out.println("movie rating"+m.getRating());
        });

    }
}
